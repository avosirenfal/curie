package ss14loaders

import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.PathWalkOption
import kotlin.io.path.readText
import kotlin.io.path.walk

private val reg = Regex("""(\S+)\s+=\s+(.+)""")
fun loadLocale(file: String): Map<String, String> {
	return file.lines()
		.map {
			val match = reg.matchEntire(it) ?: return@map null

			return@map match.groups[1]!!.value to match.groups[2]!!.value
		}
		.filterNotNull()
		.toMap()
}

@OptIn(ExperimentalPathApi::class)
fun loadAllLocaleFromPath(path: Path): Map<String, String> {
	val out = mutableMapOf<String, String>()
	path.walk(PathWalkOption.BREADTH_FIRST).forEach {
		if(it.fileName.toString().endsWith(".ftl")) {
			out.putAll(loadLocale(it.readText()))
		}
	}
	return out.toMap()
}

object SS14Locale {
	private val locale = mutableMapOf<String, String>()

	fun getLocaleString(key: String): String? = locale[key]

	fun loadAllFromPath(path: Path) {
		locale.putAll(loadAllLocaleFromPath(path))
	}
}