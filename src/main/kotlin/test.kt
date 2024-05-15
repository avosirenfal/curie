import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import com.charleskorn.kaml.*

@Serializable
data class Reagent(
	val type: String,
	val id: String,
	val name: String,
	val group: String,
	val desc: String,
	val physicalDesc: String,
	val flavor: String,
	val color: String,
	val metabolisms: Map<String, Metabolism>,
	val plantMetabolism: List<PlantMetabolism> = emptyList()
)

@Serializable
data class Metabolism(
	val effects: List<Effect>
)

@Serializable
sealed class Effect(
	val conditions: List<Condition>? = null
) {
	@Serializable
	@SerialName("!type:GenericStatusEffect")
	data class GenericStatusEffect(
		val key: String,
		val component: String
	) : Effect()

	@Serializable
	@SerialName("!type:Drunk")
	data class Drunk(
		val slurSpeech: Boolean? = null,
		val boozePower: Int? = null
	) : Effect()

	@Serializable
	@SerialName("!type:HealthChange")
	data class HealthChange(
		val damage: Damage? = null,
	) : Effect()

	@Serializable
	@SerialName("!type:Jitter")
	class Jitter : Effect()

	@Serializable
	@SerialName("!type:PopupMessage")
	data class PopupMessage(
		val type: String,
		val visualType: String,
		val messages: List<String>,
		val probability: Double
	) : Effect()

	@Serializable
	@SerialName("!type:ChemVomit")
	data class ChemVomit(
		val probability: Double
	) : Effect()

	@Serializable
	@SerialName("!type:UnhandledEffect")
	data class UnhandledEffect(
		val type: String,
		val data: JsonObject
	) : Effect()
}

@Serializable
sealed class Condition {
	@Serializable
	@SerialName("!type:ReagentThreshold")
	data class ReagentThreshold(
		val min: Int
	) : Condition()
}

@Serializable
data class Damage(
	val types: Map<String, Int>? = null,
	val groups: Map<String, Int>? = null
)

@Serializable
sealed class PlantMetabolism {
	@Serializable
	@SerialName("!type:PlantAdjustToxins")
	data class PlantAdjustToxins(
		val amount: Int
	) : PlantMetabolism()

	@Serializable
	@SerialName("!type:PlantAdjustHealth")
	data class PlantAdjustHealth(
		val amount: Int
	) : PlantMetabolism()

	@Serializable
	@SerialName("!type:UnhandledPlantMetabolism")
	data class UnhandledPlantMetabolism(
		val type: String,
		val data: JsonObject
	) : PlantMetabolism()
}

@OptIn(ExperimentalSerializationApi::class)
val yaml = Yaml(
	configuration = YamlConfiguration(
		polymorphismStyle = PolymorphismStyle.Tag
	),
)

fun main() {
	val yamlString = """
        - type: reagent
          id: Cryptobiolin
          name: reagent-name-cryptobiolin
          group: Medicine
          desc: reagent-desc-cryptobiolin
          physicalDesc: reagent-physical-desc-fizzy
          flavor: medicine
          color: "#081a80"
          metabolisms:
            Medicine:
              effects:
              - !type:GenericStatusEffect
                key: Stutter
                component: ScrambledAccent
              - !type:Drunk
                slurSpeech: false
                boozePower: 20

        - type: reagent
          id: Dylovene
          name: reagent-name-dylovene
          group: Medicine
          desc: reagent-desc-dylovene
          physicalDesc: reagent-physical-desc-translucent
          flavor: medicine
          color: "#3a1d8a"
          metabolisms:
            Medicine:
              effects:
              - !type:HealthChange
                damage:
                  types:
                    Poison: -1
              - !type:HealthChange
                conditions:
                - !type:ReagentThreshold
                  min: 20
                damage:
                  groups:
                    Brute: 2
              - !type:Jitter
                conditions:
                - !type:ReagentThreshold
                  min: 20
              - !type:PopupMessage
                conditions:
                - !type:ReagentThreshold
                  min: 20
                type: Local
                visualType: Medium
                messages: [ "generic-reagent-effect-nauseous" ]
                probability: 0.2
              - !type:ChemVomit
                conditions:
                - !type:ReagentThreshold
                  min: 20
                probability: 0.02
              - !type:Drunk
                conditions:
                - !type:ReagentThreshold
                  min: 15
          plantMetabolism:
          - !type:PlantAdjustToxins
            amount: -10
          - !type:PlantAdjustHealth
            amount: 1
    """.trimIndent()

	val reagents = yaml.decodeFromString<List<Reagent>>(yamlString)
	println(reagents)
}
