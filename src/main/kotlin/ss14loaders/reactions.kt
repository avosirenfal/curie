package ss14loaders

import SS14
import hr
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Reaction(
	val type: String,
	val id: String,
	val priority: Int = 0,
	val minTemp: Double = 0.0,
	val maxTemp: Double = Double.POSITIVE_INFINITY,
	val impact: String = "Low",
	/**
	 * If true, this reaction will only consume only integer multiples of the reactant amounts. If there are not
	 * enough reactants, the reaction does not occur. Useful for spawn-entity reactions (e.g. creating cheese).
	 */
	val quantized: Boolean = false,
	/**
	 * Determines whether this reaction creates a new chemical (false) or if it's a breakdown for existing chemicals (true)
	 * Used in the chemistry guidebook to make divisions between recipes and reaction sources.
	 *
	 * Mixing together two reagents to get a third -> false
	 * Heating a reagent to break it down into 2 different ones -> true 
	 */
	val source: Boolean = false,
	val conserveEnergy: Boolean = true,
	val requiredMixerCategories: List<String>? = null,
	val reactants: Map<String, Reactant>,
	val products: Map<String, Double>? = null,
	val effects: List<ReagentEffect>? = null,
)

@Serializable
data class Reactant(
	val amount: Double,
	val catalyst: Boolean? = false
)

@Serializable
@SerialName("!type:ExplosionReactionEffect")
data class ExplosionReactionEffect(
	val explosionType: String = "Default",
	val maxIntensity: Double = 5.0,
	val intensityPerUnit: Double = 1.0,
	val intensitySlope: Double = 1.0,
	val maxTotalIntensity: Double = 100.0,
) : ReagentEffect() {
	override fun humanReadable(): String {
		return "explodes!!" // todo
	}
}

@Serializable
@SerialName("!type:AreaReactionEffect")
data class AreaReactionEffect(
	val prototypeId: String,
	val duration: Double = 10.0,
) : ReagentEffect() {
	override fun humanReadable(): String {
		return "area reaction of ${SS14Locale.getLocaleStringSafe(prototypeId)} for ${duration.hr()} seconds"
	}
}

@Serializable
@SerialName("!type:EmpReactionEffect")
data class EmpReactionEffect(
	val rangePerUnit: Double = 0.5,
	val maxRange: Double = 10.0,
	val energyConsumption: Double = 12500.0,
	val duration: Double = 15.0,
) : ReagentEffect() {
	override fun humanReadable(): String {
		return "${duration.hr()} second EMP that drains ${energyConsumption.hr()} energy, at ${rangePerUnit.hr()} range per unit up to a maxium of ${maxRange.hr()} range"
	}
}

@Serializable
@SerialName("!type:CreateEntityReactionEffect")
data class CreateEntityReactionEffect(
	val entity: String,
) : ReagentEffect() {
	override fun humanReadable(): String {
		return "create entity ${SS14Locale.getLocaleStringSafe(entity)}"
	}
}