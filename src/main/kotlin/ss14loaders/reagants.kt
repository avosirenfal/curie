package ss14loaders

import hr
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlin.math.abs

@Serializable
data class Reagent(
    val type: String,
    val id: String,
    val name: String? = null,
    val group: String? = null,
    val desc: String? = null,
    val physicalDesc: String? = null,
    val flavor: String? = null,
    val color: String? = null,
    val metabolisms: Map<String, Metabolism>? = null,
    val plantMetabolism: List<ReagentEffect>? = null,
    val worksOnTheDead: Boolean = false,
    val recognizable: Boolean = false
)

@Serializable
data class Metabolism(
    val metabolismRate: Double = 0.5,
    val effects: List<ReagentEffect>
)

@Serializable
sealed class ReagentEffect(
    val probability: Double = 1.0,
    val conditions: List<Condition>? = null
) {
    abstract fun humanReadable(): String

    companion object {
        val serialModule = SerializersModule {
            this.polymorphic(ReagentEffect::class) {
                defaultDeserializer { UnknownEffect.serializer() }
            }
        }
    }
}

@Serializable
class UnknownEffect : ReagentEffect() {
    override fun humanReadable(): String = "unknown/unhandled"
}

private fun minMaxString(min: Double?, max: Double?, str: String, unit: String = "u"): String? {
    if(min != null && max != null)
        return "between ${min.hr()}${unit} and ${max.hr()}${unit}$str"

    if(min != null)
        return "at least ${min.hr()}${unit}$str"

    if(max != null)
        return "less than ${max.hr()}${unit}$str"

    return null
}

@Serializable
sealed class Condition {
    abstract fun humanDescription(): String

    @Serializable
    @SerialName("!type:ReagentThreshold")
    data class ReagentThreshold(
        val min: Double? = null,
        val max: Double? = null,
        val reagent: String? = null
    ) : Condition() {
        override fun humanDescription(): String {
            return minMaxString(min, max, if(reagent == null) "" else " of $reagent")
                ?: "ReagentThreshold unclear"
        }
    }

    @Serializable
    @SerialName("!type:TotalDamage")
    data class TotalDamage(
        val max: Double? = null,
        val min: Double? = null
    ) : Condition() {
        override fun humanDescription(): String {
            return minMaxString(min, max, " of total damage")
                ?: "TotalDamage unclear"
        }
    }

    @Serializable
    @SerialName("!type:Temperature")
    data class Temperature(
        val min: Double? = null,
        val max: Double? = null
    ) : Condition() {
        override fun humanDescription(): String {
            return minMaxString(min, max, "", "K")?.let { "temperature $it" }
                ?: "Temperature unclear"
        }
    }

    @Serializable
    @SerialName("!type:OrganType")
    data class OrganType(
        val type: String
    ) : Condition() {
        override fun humanDescription(): String {
            return "with organ type: $type"
        }
    }

    @Serializable
    @SerialName("!type:HasTag")
    data class HasTag(
        val invert: Boolean? = null,
        val tag: String,
    ) : Condition() {
        override fun humanDescription(): String {
            if(invert == true)
                return "does not have tag: $tag"

            return "has tag: $tag"
        }
    }

    @Serializable
    @SerialName("!type:MobStateCondition")
    data class MobStateCondition(
        val mobstate: String,
    ) : Condition(){
        override fun humanDescription(): String {
            return "has mob state [$mobstate]"
        }
    }
}

@Serializable
@SerialName("!type:GenericStatusEffect")
data class GenericStatusEffect(
    val key: String,
    val component: String? = null,
    val time: Double? = null,
    val type: String? = null
) : ReagentEffect() {
    override fun humanReadable(): String {
        val name = run {
            if(key == component)
                key
            else if (component != null)
                "$key / $component"
            else
                key
        }
        val time = if(time == null) " " else " ${time.hr()} seconds of "

        return when(type) {
            null -> "inflict${time}generic status effect $name"
            "Remove" -> "removes${time}generic status effect $name"
            "Add" -> "adds${time}generic status effect $name"
            else -> "$type${time}generic status effect $name"
        }
    }
}

@Serializable
@SerialName("!type:Drunk")
data class Drunk(
    val slurSpeech: Boolean? = null,
    val boozePower: Int? = null,
) : ReagentEffect() {
    override fun humanReadable(): String {
        return "inflicts drunk"
    }
}

@Serializable
@SerialName("!type:HealthChange")
data class HealthChange(
    val damage: HealthDamage,
) : ReagentEffect() {
    fun healthValues(): Map<String, Double> =
        ((this.damage.groups ?: mapOf()).entries + (this.damage.types ?: mapOf()).entries).associate { it.key to it.value }

    override fun humanReadable(): String {
        return this.healthValues().map {
            val value = it.value * -1

            if(value > 0)
                "heals ${value.hr()} ${it.key}"
            else
                "inflicts ${(value * -1).hr()} ${it.key} damage"
        }.joinToString(", ")
    }
}

@Serializable
@SerialName("!type:MovespeedModifier")
data class MovespeedModifier(
    val walkSpeedModifier: Double?,
    val sprintSpeedModifier: Double?,
) : ReagentEffect() {
    override fun humanReadable(): String {
        val msgs = mutableListOf<String>()

        if(walkSpeedModifier != null) {
            val verb = if(walkSpeedModifier > 1.0) "increases" else "decreases"
            msgs.add("$verb walk speed by ${((abs(walkSpeedModifier) - 1) * 100).hr()}%")
        }

        if(sprintSpeedModifier != null) {
            val verb = if(sprintSpeedModifier > 1.0) "increases" else "decreases"
            msgs.add("$verb sprint speed by ${((abs(sprintSpeedModifier) - 1) * 100).hr()}%")
        }

        return msgs.joinToString(", ")
    }
}

@Serializable
data class HealthDamage(
    val types: Map<String, Double>? = null,
    val groups: Map<String, Double>? = null
)

@Serializable
@SerialName("!type:Jitter")
class Jitter() : ReagentEffect() {
    override fun humanReadable(): String {
        return "inflict jitter"
    }
}

@Serializable
@SerialName("!type:PopupMessage")
data class PopupMessage(
    val type: String? = null,
    val messageType: String? = null,
    val visualType: String? = null,
    val messages: List<String>,
) : ReagentEffect() {
    override fun humanReadable(): String {
        return "popup message: ${messages.joinToString(" / ") { "\"" + (SS14Locale.getLocaleString(it) ?: it)  + "\"" } }"
    }
}

@Serializable
@SerialName("!type:ChemVomit")
class ChemVomit() : ReagentEffect() {
    override fun humanReadable(): String {
        return "inflicts vomit"
    }
}

@Serializable
@SerialName("!type:AdjustTemperature")
data class AdjustTemperature(
    val amount: Double
) : ReagentEffect() {
    override fun humanReadable(): String {
        return if(amount > 0)
            "+${amount.hr()} thermal energy"
        else
            "-${(amount * -1).hr()} thermal energy"
    }
}

@Serializable
@SerialName("!type:AdjustReagent")
data class AdjustReagent(
    val amount: Double,
    val reagent: String
) : ReagentEffect() {
    override fun humanReadable(): String {
        return if(amount > 0)
            "add ${amount.hr()}u of reagent ${SS14Locale.getLocaleString(reagent) ?: reagent}"
        else
            "remove ${(amount * -1).hr()}u of reagent ${SS14Locale.getLocaleString(reagent) ?: reagent}"
    }
}

@Serializable
@SerialName("!type:ModifyBloodLevel")
data class ModifyBloodLevel(
    val amount: Double,
) : ReagentEffect() {
    override fun humanReadable(): String {
        return if(amount > 0)
            "add ${amount.hr()} to blood level"
        else
            "remove ${(amount * -1).hr()} from blood level"
    }
}

@Serializable
@SerialName("!type:SatiateThirst")
data class SatiateThirst(
    val factor: Double? = null,
) : ReagentEffect() {
    override fun humanReadable(): String {
        if(factor == null)
            return "satiate thirst (completely? 1u? unclear!)"

        return if(factor > 0)
            "satiate ${factor.hr()} thirst"
        else
            "cause ${(factor * -1).hr()} thirst"
    }
}

@Serializable
@SerialName("!type:SatiateHunger")
data class SatiateHunger(
    val factor: Double? = null,
) : ReagentEffect() {
    override fun humanReadable(): String {
        if(factor == null)
            return "satiate hunger (completely? 1u? unclear!)"

        return if(factor > 0)
            "satiate ${factor.hr()} hunger"
        else
            "cause ${(factor * -1).hr()} hunger"
    }
}

@Serializable
@SerialName("!type:ChemHealEyeDamage")
class ChemHealEyeDamage() : ReagentEffect() {
    override fun humanReadable(): String {
        return "heal eye damage"
    }
}

@Serializable
@SerialName("!type:MakeSentient")
class MakeSentient() : ReagentEffect() {
    override fun humanReadable(): String {
        return "causes sentience"
    }
}

@Serializable
@SerialName("!type:ResetNarcolepsy")
class ResetNarcolepsy() : ReagentEffect() {
    override fun humanReadable(): String {
        return "reset narcolepsy"
    }
}

@Serializable
@SerialName("!type:ModifyBleedAmount")
data class ModifyBleedAmount(
    val amount: Double,
) : ReagentEffect() {
    override fun humanReadable(): String {
        return if(amount > 0)
            "cause ${amount.hr()} bleeding"
        else
            "reduce bleeding by ${(amount * -1).hr()}"
    }
}

@Serializable
@SerialName("!type:CauseZombieInfection")
class CauseZombieInfection() : ReagentEffect() {
    override fun humanReadable(): String {
        return "cause zombie infection"
    }
}

@Serializable
@SerialName("!type:CureZombieInfection")
class CureZombieInfection() : ReagentEffect() {
    override fun humanReadable(): String {
        return "cure zombie infection"
    }
}


@Serializable
@SerialName("!type:Emote")
data class Emote(
    val emote: String,
) : ReagentEffect() {
    override fun humanReadable(): String {
        return "causes emote $emote"
    }
}

@Serializable
@SerialName("!type:PlantAdjustToxins")
data class PlantAdjustToxins(
    val amount: Double
) : ReagentEffect() {
    override fun humanReadable(): String {
        return "PlantAdjustToxins"
    }
}

@Serializable
@SerialName("!type:PlantAdjustHealth")
data class PlantAdjustHealth(
    val amount: Double
) : ReagentEffect() {
    override fun humanReadable(): String {
        return "PlantAdjustHealth"
    }
}

@Serializable
@SerialName("!type:Polymorph")
data class Polymorph(
    val prototype: String,
) : ReagentEffect() {
    override fun humanReadable(): String {
        return "polymorph prototype to $prototype"
    }
}

@Serializable
@SerialName("!type:ChemCleanBloodstream")
data class ChemCleanBloodstream(
    val cleanseRate: Double,
) : ReagentEffect() {
    override fun humanReadable(): String {
        return "cleanse bloodstream by ${cleanseRate.hr()}"
    }
}

@Serializable
@SerialName("!type:Electrocute")
class Electrocute() : ReagentEffect() {
    override fun humanReadable(): String {
        return "electrocute"
    }
}

@Serializable
@SerialName("!type:Oxygenate")
class Oxygenate() : ReagentEffect() {
    override fun humanReadable(): String {
        return "oxygenate"
    }
}

@Serializable
@SerialName("!type:ModifyLungGas")
data class ModifyLungGas(
    val ratios: Map<String, Double>
) : ReagentEffect() {
    override fun humanReadable(): String {
        val ratio_str = ratios.map {
            if(it.value > 0)
                return@map "add ${it.value.hr()} ${it.key} to lungs"
            else
                return@map "remove ${(it.value * -1).hr()} ${it.key} from lungs"
        }

        return ratio_str.joinToString(", ")
    }
}

@Serializable
@SerialName("!type:AdjustAlert")
data class AdjustAlert(
    val alertType: String,
    val clear: Boolean,
    val time: Double,
) : ReagentEffect() {
    override fun humanReadable(): String {
        return "trigger alert type $alertType for ${time.hr()} seconds"
    }
}

@Serializable
@SerialName("!type:FlammableReaction")
data class FlammableReaction(
    val multiplier: Double,
) : ReagentEffect() {
    override fun humanReadable(): String {
        return "flammable reaction with multiplier ${multiplier.hr()}"
    }
}


