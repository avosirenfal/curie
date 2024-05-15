package ss14loaders

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@Serializable
data class Reagent(
    val type: String,
    var id: String,
    var name: String? = null,
    var group: String? = null,
    var desc: String? = null,
    var physicalDesc: String? = null,
    var flavor: String? = null,
    var color: String? = null,
    var metabolisms: Map<String, Metabolism>? = null,
    var plantMetabolism: List<Effect>? = null
)

@Serializable
data class Metabolism(
    var effects: List<Effect>
)

@Serializable
open class Effect(
    val conditions: List<Condition>? = null
) {
    companion object {
        val serialModule = SerializersModule {
            this.polymorphic(Effect::class) {
//                subclass(Cat::class)
//                subclass(Dog::class)
                defaultDeserializer { UnknownEffect.serializer() }
            }
        }
    }
}

@Serializable
class UnknownEffect : Effect()

@Serializable
sealed class Condition {
    @Serializable
    @SerialName("!type:ReagentThreshold")
    data class ReagentThreshold(
        val min: Double? = null,
        val max: Double? = null
    ) : Condition()

    @Serializable
    @SerialName("!type:TotalDamage")
    data class TotalDamage(
        val max: Double,
    ) : Condition()

    @Serializable
    @SerialName("!type:Temperature")
    data class Temperature(
        val min: Double? = null,
        val max: Double? = null
    ) : Condition()

    @Serializable
    @SerialName("!type:OrganType")
    data class OrganType(
        val type: String
    ) : Condition()

    @Serializable
    @SerialName("!type:HasTag")
    data class HasTag(
        val invert: Boolean? = null,
        val tag: String,
    ) : Condition()

    @Serializable
    @SerialName("!type:MobStateCondition")
    data class MobStateCondition(
        val mobstate: String,
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