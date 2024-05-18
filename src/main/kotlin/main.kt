import kotlinx.serialization.*
import com.charleskorn.kaml.*
import kotlinx.serialization.modules.SerializersModule
import ss14loaders.*
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.listDirectoryEntries
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

	// TODO: get heals
	return "OD ${threshold.min.hr()}u"
}

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

	val reactions = reactions_path.listDirectoryEntries("*.yml").associate {
		println(it)
		it.fileName.toString() to yaml.decodeFromString<List<Reaction>>(it.readText().trim())
	}

	// note: default metabolic rate is 0.5

	// TODO: metabolic rate
	for ((src, reagent) in reagents.entries.map { it.value.map { reagent -> it.key to reagent } }.flatten()) {
		if(reagent.metabolisms == null)
			continue

		if(src != "medicine.yml")
			continue

		val metabolismLookup = reagent.metabolisms
			.map { it.value.effects.map { eff -> eff to it.value  } }
			.flatten().toMap()
		val allEffects = reagent.metabolisms.map { it.value.effects }.flatten()
		val healthEffects = allEffects.filterIsInstance<HealthChange>()

		val name = SS14Locale.getLocaleString(reagent.name ?: "notfound")!!
			.split(" ")
			.joinToString(" ") { it.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } }

		println(buildString {
			append(name)
			append(" / ")
			append(healthEffects.firstOrNull { it.overdoseString() != null }?.overdoseString() ?: "Cannot Overdose")

			if(reagent.worksOnTheDead)
				append(" / works on the dead")

//			append(" - $src")
		})
		println(SS14Locale.getLocaleString(reagent.desc!!)!!)
//		if(!reagent.recognizable && reagent.physicalDesc != null)
//			println("    physical descrption: " + SS14Locale.getLocaleString(reagent.physicalDesc)!!)

		// print all heal effects first
		healthEffects.filter { it.healthValues().isNotEmpty() }
			.forEach {
				println(buildString {
					append("    * ")

					if(it.conditions == null)
						append("always")
					else
						append(it.conditions.joinToString(", ") { it.humanDescription() })

					append(": ")

					append(it.healthValues().map {
						val value = it.value * -1

						if(value > 0)
							"heals ${value.hr()} ${it.key}"
						else
							"inflicts ${(value * -1).hr()} ${it.key} damage"
					}.joinToString(", "))
				})
			}

		// print all other effects
		allEffects
			.filter { it !is HealthChange }
			.forEach {
				println(buildString {
					append("    - ")

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

		println()
	}
}
