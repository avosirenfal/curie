import kotlinx.serialization.*
import com.charleskorn.kaml.*
import kotlinx.serialization.modules.SerializersModule
import ss14loaders.*
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.readText

@OptIn(ExperimentalSerializationApi::class)
val yaml = Yaml(
	configuration = YamlConfiguration(
		strictMode = false,
		polymorphismStyle = PolymorphismStyle.Tag
	),
	serializersModule = SerializersModule {
		include(ReagentEffect.serialModule)
	}
)

fun HealthChange.healthValues(): Map<String, Double> =
	((this.damage.groups ?: mapOf()).entries + (this.damage.types ?: mapOf()).entries).associate { it.key to it.value }

fun HealthChange.overdoseString(): String? {
	if(this.conditions == null || this.conditions.size != 1
		|| this.conditions[0] !is Condition.ReagentThreshold)
		return null

	// must be triggered by too much of this specific reagent
	val threshold = this.conditions[0] as Condition.ReagentThreshold
	if(threshold.reagent != null || threshold.min == null || threshold.min <= 0.0)
		return null

	val damageTypes = this.healthValues().filter { it.value > 0 }

	// this heals, it doesn't dmage
	if(damageTypes.isEmpty())
		return null

	return "OD ${threshold.min.hr()}u"
}

class SS14DataContainer(
	val reactions: Map<String, List<Reaction>>,
	val reagents: Map<String, Reagent>
) {
	fun getReactionsFor(id: String): List<Reaction>? = this.reactions[id]
	fun getReagent(id: String): Reagent = this.reagents.getValue(id)
}

object SS14 {
	var data: SS14DataContainer? = null
	fun setContainer(data: SS14DataContainer) {
		this.data = data
	}
}

private fun localizeReagentId(id: String): String {
	return SS14Locale.getLocaleStringSafe(SS14.data?.reagents?.get(id)?.name ?: id)
		.lowercase()
		.split(" ")
		.joinToString(" ") { it.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } }
}

sealed class Node {
	abstract val id: String
	/**
	 * item should come from a jug or otherwise be provided - it cannot be crafted with chemistry
	 */
	class Provided(override val id: String) : Node()

	/**
	 * this should be crafted with chemistry
	 *
	 * note: we need to know the entire set of craftables to resolve this to a string later
	 */
	class Crafted(override val id: String, val reaction: Reaction, val reagentSources: Map<String, List<Node>>) : Node() {
		// must be generated from the tree first + list of jugs
		fun stringify(crafted: Set<String>): String {
			fun String.desc(quantity: Double): String {
				val localized = localizeReagentId(this)

				if(this@desc !in crafted)
					return "${quantity.hr()} <$localized>"
				return "${quantity.hr()} $localized"
			}

			val recipe = reaction.reactants.map { it.key.desc(it.value.amount) }.joinToString(", ") + " => " + id.desc(reaction.products!!.getValue(id))
			val machine = reaction.requiredMixerCategories?.joinToString(", ")
			val heat = if(reaction.minTemp > 0) reaction.minTemp else null

			return buildString {
				append(recipe)
				if(machine != null)
					append(" [machine: $machine]")

				if(heat != null)
					append(" <min temp: ${heat.hr()}K>")
			}
		}
	}

	companion object {
		fun getSourcesFor(
			id: String,
			data: SS14DataContainer,
			providedExternally: Set<String>,
			refs: MutableMap<String, List<Node>> = mutableMapOf(),
		): List<Node> {
			return refs.getOrPut(id) {
				val reactions = data.getReactionsFor(id)

				if(reactions?.isEmpty() != false || id in providedExternally)
					return listOf(Provided(id))

				reactions.map { reaction ->
					val sources = reaction.reactants.map { src ->
						src.key to refs.getOrPut(src.key) {getSourcesFor(src.key, data, providedExternally, refs)}
					}.toMap()
					Crafted(id, reaction, sources)
				}
			}
		}

		private fun getComponentsFromTree(node: Node, found: Set<String> = setOf(), craftedOnly: Boolean): Set<String> {
			return when(node) {
				is Provided -> if(craftedOnly) found else found + setOf(node.id)
				is Crafted -> {
					val children = node.reagentSources.filter { it.key !in found }
					val search = children.flatMap { it.value.flatMap { getComponentsFromTree(it, found + setOf(node.id), craftedOnly) } }.toSet()

					return found + search
				}
			}
		}

		private fun Map<String, Int>.updateIt(id: String, depth: Int): Map<String, Int> {
			val current = this[id]

			return if(current == null || depth > current) {
				val copy = this.toMutableMap()
				copy[id] = depth
				copy
			} else {
				this
			}
		}

		private fun getTreeDepths(node: Node, depth: Int=0, found: Map<String, Int> = mapOf()): Map<String, Int> {
			return when(node) {
				is Provided -> found.updateIt(node.id, depth)
				is Crafted -> {
					var h = found.updateIt(node.id, depth)
					val d = depth+1

					node.reagentSources.keys.forEach {
						h = h.updateIt(it, d)
					}
					val children = node.reagentSources.filter { it.key !in found }
					for(v in children) {
						for(item in v.value) {
							h = getTreeDepths(item, d + 1, h)
						}
					}

					return h
				}
			}
		}

		private data class ResultString(val productid: String, val line: String)
		private data class TreeStringResult(val items: List<ResultString>, val seen: Set<String>)
		private fun treeToString(node: Crafted, crafted: Set<String>, seen: Set<String> = setOf()): TreeStringResult {
			if(node.id in seen)
				return TreeStringResult(listOf(), setOf())

			val newseen = (seen + setOf(node.id)).toMutableSet()

			return TreeStringResult(
				buildList {
					add(ResultString(node.id, node.stringify(crafted)))

					for(nodes in node.reagentSources.values) {
						for(subnode in nodes) {
							if(subnode is Crafted) {
								val res = treeToString(subnode, crafted, newseen)
								addAll(res.items)
								newseen.addAll(res.seen)
							}
						}
					}
				}.reversed(),
				seen
			)
		}

		fun getRecipeList(id: String, data: SS14DataContainer, providedExternally: Set<String>): String {
			val tree = getSourcesFor(id, data, providedExternally)

			return tree.mapNotNull { node ->
				if (node is Crafted) {
					val sort = getTreeDepths(node)
 					val crafted = getComponentsFromTree(node, craftedOnly=true).toSet() + setOf(id)
					val notCrafted = (getComponentsFromTree(node, craftedOnly=false).toSet() - crafted)
						.joinToString(", ") { id ->
							val name = localizeReagentId(id)
							val source = sources.get(id)

							if(source != null)
								"$name [$source]"
							else
								name
						}
					val buyString = "required: $notCrafted"

					val hit = mutableSetOf<String>()
					val results = treeToString(node, crafted).items
						.sortedBy { sort[it.productid] ?: 1 }
						.reversed()
						// TODO: I don't want to figure out why we occasionally get duplicates
						.filter { hit.add(it.line) }
						.joinToString("\n   ") { it.line }

					"$buyString\n   $results"
				} else
					null
			}.joinToString("\n\n")
		}
	}
}

val jugSet = setOf("Aluminium", "Carbon", "Chlorine", "Copper", "Ethanol", "Fluorine", "Hydrogen", "Iodine", "Iron", "Lithium", "Mercury", "Nitrogen", "Oxygen", "Phosphorus", "Potassium", "Radium", "Silicon", "Sodium", "Sugar", "Sulfur", "Water", "Blood", "WeldingFuel", "Plasma")
val sources = mapOf(
	"P" to setOf("Aluminium", "Carbon", "Chlorine", "Fluorine", "Iodine", "Phosphorus", "Sulfur", "Silicon", "Oxygen", "Nitrogen"),
	"S" to setOf("Hydrogen", "Lithium", "Sodium", "Potassium", "Radium", "Sugar", "Ethanol"),
	"D" to setOf("Iron", "Copper", "Gold", "Mercury", "Silver"),
	"Syndicate" to setOf("Vestine"),
	"Botany" to setOf("Omnizine", "Stellibinin", "Aloe"),
	"Grind" to setOf("Plasma", "Uranium")
).flatMap { x -> x.value.map { it to x.key } }.toMap()

// TODO: read body/organs/X and do results on a per-race basis
fun main(args: Array<String>) {
	val ss14_path = Path(args[0])
	val prototypes_path = Path(ss14_path.toString(), "Resources", "Prototypes")
	val reagents_path = Path(prototypes_path.toString(), "Reagents")
	val reactions_path = Path(prototypes_path.toString(), "Recipes", "Reactions")
	SS14Locale.loadAllFromPath(Path(ss14_path.toString(), "Resources", "Locale", "en-US"))

	val reagents = reagents_path.listDirectoryEntries("*.yml").associate {
//		println(it)
		it.fileName.toString() to yaml.decodeFromString<List<Reagent>>(it.readText())
	}

	val reactionLookup = reactions_path
		.listDirectoryEntries("*.yml")
		.flatMap { yaml.decodeFromString<List<Reaction>>(it.readText().trim()) }
		.flatMap { it.products?.map { (product, _) -> product to it } ?: listOf() }
		.groupBy({it.first}, {it.second})

	val data = SS14DataContainer(reactionLookup, reagents.flatMap { it.value }.associateBy { it.id })
	SS14.setContainer(data)

	for ((src, reagent) in reagents.entries.map { it.value.map { reagent -> it.key to reagent } }.flatten()) {
		if(reagent.abstract)
			continue

//		if(reagent.metabolisms == null)
//			continue

//		if(src != "medicine.yml")
//			continue

		val metabolismLookup = reagent.metabolisms
			?.map { it.value.effects.map { eff -> eff to it.value  } }
			?.flatten()?.toMap() ?: mapOf()
		val allEffects = reagent.metabolisms?.map { it.value.effects }?.flatten() ?: listOf()

		val name = SS14Locale.getLocaleString(reagent.name ?: "notfound")!!
			.split(" ")
			.joinToString(" ") { it.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } }

		println(buildString {
			append(name)
			append(" / ")
			append(allEffects.filterIsInstance<HealthChange>().firstOrNull { it.overdoseString() != null }?.overdoseString() ?: "Cannot Overdose")

			append(" [${Path(src).nameWithoutExtension}]")
		})

		if(reagent.worksOnTheDead)
			println("(works on the dead)")

		println(reagent.desc?.let { SS14Locale.getLocaleString(it) } ?: "No description.")
//		println("    " + reactionLookup.getReactionTreeFor(reagent.id, jugSet).joinToString("\n    "))

//		if(!reagent.recognizable && reagent.physicalDesc != null)
//			println("    physical descrption: " + SS14Locale.getLocaleString(reagent.physicalDesc)!!)

		allEffects
			.sortedBy { it !is HealthChange }
			.forEach {
				println(buildString {
					if(it is HealthChange)
						append("    * ")
					else
						append("    - ")

					val mr = metabolismLookup.getValue(it)

					if(mr.metabolismRate != 0.5)
						append("[${mr.metabolismRate}u/tick] ")

					if(it.conditions == null)
						append("always")
					else
						append(it.conditions.joinToString(", ") { it.humanDescription() })

					append(": ")
					append(it.humanReadable())

					if(it.probability != 1.0)
						append(" (${(it.probability * 100).hr()}% chance)")
				})
			}

		val recipes = Node.getRecipeList(reagent.id, data, jugSet).prependIndent("    ")

		if(recipes.trim().isNotEmpty()) {
			if(allEffects.isNotEmpty())
				println()

			println(recipes)
		}

		println()
	}
}
