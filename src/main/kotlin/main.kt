import kotlinx.serialization.*
import com.charleskorn.kaml.*
import kotlinx.serialization.modules.SerializersModule
import ss14loaders.Condition
import ss14loaders.Effect
import ss14loaders.HealthChange
import ss14loaders.Reagent
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
		include(Effect.serialModule)
	}
)

fun HealthChange.overdoseString(): String? {
	if(this.conditions == null || this.conditions.size != 1
		|| this.conditions[0] !is Condition.ReagentThreshold)
		return null

	// must be triggered by too much of this specific reagant
	val threshold = this.conditions[0] as Condition.ReagentThreshold
	if(threshold.reagant != null || threshold.min == null)
		return null

	val damageTypes = ((this.damage.groups ?: mapOf()).entries + (this.damage.types ?: mapOf()).entries)
		.filter { it.value > 0 }

	// this heals, it doesn't dmage
	if(damageTypes.isEmpty())
		return null

	// TODO: get heals
	return "OD ${threshold.min.hr()}u"
}

fun Double.hr(): String {
	return if (this % 1.0 == 0.0) {
		this.toInt().toString()
	} else {
		this.toString()
	}
}

fun main(args: Array<String>) {
	val ss14_path = Path(args[0])
	val prototypes_path = Path(ss14_path.toString(), "Resources", "Prototypes")
	val reagants_path = Path(prototypes_path.toString(), "Reagents")

	val reagants = reagants_path.listDirectoryEntries("*.yml").map {
		it.fileName.toString() to yaml.decodeFromString<List<Reagent>>(it.readText())
	}.toMap()

	reagants.get("medicine.yml")!!.forEach { reagant ->
		val effects = reagant.metabolisms?.get("Medicine")?.effects ?: return@forEach
		val change = effects.filterIsInstance<HealthChange>().filter { it.overdoseString() != null }

		if(change.isEmpty())
			return@forEach

		println(reagant.id + " " + change[0].overdoseString())
	}
//	val reagents = yaml.decodeFromString<List<Reagent>>(yamlString)
//	println(reagents)
}
