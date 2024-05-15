package ss14loaders

import kotlinx.serialization.Serializable

@Serializable
data class Reaction(
	val type: String,
	val id: String,
	val reactants: Map<String, ChemicalAmount>,
	val products: Map<String, Int>
)

@Serializable
data class ChemicalAmount(
	val amount: Int
)

@Serializable
data class Reactions(val reactions: List<Reaction>)