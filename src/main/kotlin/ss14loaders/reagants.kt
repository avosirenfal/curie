package ss14loaders

import hr
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

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
    val plantMetabolism: List<Effect>? = null
)

@Serializable
data class Metabolism(
    val effects: List<Effect>
)

@Serializable
sealed class Effect(
    val probability: Double? = null,
    val conditions: List<Condition>? = null
) {
    abstract fun humanReadable(): String

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
class UnknownEffect : Effect() {
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
        val reagant: String? = null
    ) : Condition() {
        override fun humanDescription(): String {
            return minMaxString(min, max, if(reagant == null) "" else " of $reagant")
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
            return minMaxString(min, max, " temperature", "")
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
            return "has mob state: $mobstate"
        }
    }
}

@Serializable
@SerialName("!type:GenericStatusEffect")
data class GenericStatusEffect(
    val key: String,
    val component: String? = null
) : Effect() {
    override fun humanReadable(): String {
        val name = run {
            if(key == component)
                key
            else if (component != null)
                "$key / $component"
            else
                key
        }

        return "inflict generic status effect ($name)"
    }
}

@Serializable
@SerialName("!type:Drunk")
data class Drunk(
    val slurSpeech: Boolean? = null,
    val boozePower: Int? = null,
) : Effect() {
    override fun humanReadable(): String {
        return "inflicts drunk"
    }
}

@Serializable
@SerialName("!type:HealthChange")
data class HealthChange(
    val damage: HealthDamage,
) : Effect() {
    override fun humanReadable(): String {
        return "healthchange"
    }
}

@Serializable
data class HealthDamage(
    val types: Map<String, Double>? = null,
    val groups: Map<String, Double>? = null
)

@Serializable
@SerialName("!type:Jitter")
class Jitter() : Effect() {
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
) : Effect() {
    override fun humanReadable(): String {
        return "popup message: ${messages.joinToString(" / ") { "\"" + (SS14Locale.getLocaleString(it) ?: it)  + "\"" } }"
    }
}

@Serializable
@SerialName("!type:ChemVomit")
class ChemVomit() : Effect() {
    override fun humanReadable(): String {
        return "inflicts vomit"
    }
}

@Serializable
@SerialName("!type:AdjustTemperature")
data class AdjustTemperature(
    val amount: Double
) : Effect() {
    override fun humanReadable(): String {
        return if(amount > 0)
            "temperature +${amount.hr()}"
        else
            "temperature -${(amount * -1).hr()}"
    }
}

@Serializable
@SerialName("!type:AdjustReagent")
data class AdjustReagent(
    val amount: Double,
    val reagent: String
) : Effect() {
    override fun humanReadable(): String {
        return if(amount > 0)
            "adjust reagant ${SS14Locale.getLocaleString(reagent) ?: reagent} +${amount.hr()}u"
        else
            "adjust reagant ${SS14Locale.getLocaleString(reagent) ?: reagent} -${(amount * -1).hr()}u"
    }
}

@Serializable
@SerialName("!type:SatiateThirst")
data class SatiateThirst(
    val factor: Double? = null,
) : Effect() {
    override fun humanReadable(): String {
        if(factor == null)
            return "satiate thirst (completely? 1u? unclear!)"

        return if(factor > 0)
            "satiate thirst +${factor.hr()}"
        else
            "cause thirst -${(factor * -1).hr()}"
    }
}

@Serializable
@SerialName("!type:SatiateHunger")
data class SatiateHunger(
    val factor: Double? = null,
) : Effect() {
    override fun humanReadable(): String {
        if(factor == null)
            return "satiate hunger (completely? 1u? unclear!)"

        return if(factor > 0)
            "satiate hunger +${factor.hr()}"
        else
            "cause hunger -${(factor * -1).hr()}"
    }
}

@Serializable
@SerialName("!type:ChemHealEyeDamage")
class ChemHealEyeDamage() : Effect() {
    override fun humanReadable(): String {
        return "heal eye damage"
    }
}

@Serializable
@SerialName("!type:MakeSentient")
class MakeSentient() : Effect() {
    override fun humanReadable(): String {
        return "causes sentience"
    }
}

@Serializable
@SerialName("!type:ResetNarcolepsy")
class ResetNarcolepsy() : Effect() {
    override fun humanReadable(): String {
        return "reset narcolepsy"
    }
}

@Serializable
@SerialName("!type:ModifyBleedAmount")
data class ModifyBleedAmount(
    val amount: Double,
) : Effect() {
    override fun humanReadable(): String {
        return if(amount > 0)
            "modify bleed amount +${amount.hr()}"
        else
            "modify bleed amount -${(amount * -1).hr()}"
    }
}

@Serializable
@SerialName("!type:CureZombieInfection")
class CureZombieInfection() : Effect() {
    override fun humanReadable(): String {
        return "cure zombie infection"
    }
}


@Serializable
@SerialName("!type:Emote")
data class Emote(
    val emote: String,
) : Effect() {
    override fun humanReadable(): String {
        return "causes emote: $emote"
    }
}

@Serializable
@SerialName("!type:PlantAdjustToxins")
data class PlantAdjustToxins(
    val amount: Double
) : Effect() {
    override fun humanReadable(): String {
        return "PlantAdjustToxins"
    }
}

@Serializable
@SerialName("!type:PlantAdjustHealth")
data class PlantAdjustHealth(
    val amount: Double
) : Effect() {
    override fun humanReadable(): String {
        return "PlantAdjustHealth"
    }
}
