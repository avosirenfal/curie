fun Double.hr(): String {
	val ret = "%.1f".format(this)
	if(ret.endsWith(".0"))
		return ret.substring(0, ret.length -2)
	return ret
}