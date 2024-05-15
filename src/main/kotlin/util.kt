fun Double.hr(): String {
	return if (this % 1.0 == 0.0) {
		this.toInt().toString()
	} else {
		this.toString()
	}
}