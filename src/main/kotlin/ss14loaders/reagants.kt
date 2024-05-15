package ss14loaders

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Reagent(
    var id: String,
    var name: String,
    var group: String,
    var desc: String,
    var physicalDesc: String,
    var flavor: String,
    var color: String,
    var metabolisms: Map<String, Metabolism>,
    var plantMetabolism: List<Effect>? // Can be nullable in case a reagent doesn't have this property
)

@Serializable
data class Metabolism(
    var effects: List<Effect>
)

@Serializable
sealed class Effect(
    val conditions: List<Condition>? = null
)

@Serializable
sealed class Condition {
    @Serializable
    @SerialName("!type:ReagentThreshold")
    data class ReagentThreshold(
        val min: Int
    ) : Condition()
}

@Serializable
@SerialName("!type:GenericStatusEffect")
data class GenericStatusEffect(
    var key: String,
    var component: String
) : Effect()

@Serializable
@SerialName("!type:Drunk")
data class Drunk(
    var slurSpeech: Boolean? = null,
    var boozePower: Int? = null,
) : Effect()

@Serializable
@SerialName("!type:HealthChange")
data class HealthChange(
    var damage: HealthDamage,
) : Effect()

@Serializable
data class HealthDamage(
    var types: Map<String, Int>? = null,
    var groups: Map<String, Int>? = null
)

@Serializable
@SerialName("!type:Jitter")
class Jitter() : Effect()

@Serializable
@SerialName("!type:PopupMessage")
data class PopupMessage(
    var messageType: String,
    var visualType: String,
    var messages: List<String>,
    var probability: Double
) : Effect()

@Serializable
@SerialName("!type:ChemVomit")
data class ChemVomit(
    var probability: Double
) : Effect()

@Serializable
@SerialName("!type:PlantAdjustToxins")
data class PlantAdjustToxins(
    var amount: Int
) : Effect()

@Serializable
@SerialName("!type:PlantAdjustHealth")
data class PlantAdjustHealth(
    var amount: Int
) : Effect()