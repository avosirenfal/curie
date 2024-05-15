import kotlinx.serialization.*
import com.charleskorn.kaml.*
import kotlinx.serialization.modules.SerializersModule
import ss14loaders.Effect
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

fun main(args: Array<String>) {
	val ss14_path = Path(args[0])
	val prototypes_path = Path(ss14_path.toString(), "Resources", "Prototypes")
	val reagants_path = Path(prototypes_path.toString(), "Reagents")

	val reagants = reagants_path.listDirectoryEntries("*.yml").map {
		yaml.decodeFromString<List<Reagent>>(it.readText())
	}

	println("lol")
//	val reagents = yaml.decodeFromString<List<Reagent>>(yamlString)
//	println(reagents)
}
