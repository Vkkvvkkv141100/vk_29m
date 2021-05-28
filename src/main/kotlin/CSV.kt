class CSV(private val stringCSV: String) { // Экземпляр этого класса принимает строку данных в формате CSV и парсит ее значения
	val list: List<String>
	val header: List<String>
	private val info: List<String>
	val splitedInfo: MutableList<List<String>>

	init {
		this.list = this.stringCSV.split("\n") // ALL CSV ROWS
		this.header = this.list[0].split(";") // ROW HEAD
		this.info = this.list.slice(1..this.list.lastIndex) // ROW INFO
		this.splitedInfo = this.info.map { // SPLITED ROW INFO
			it.split(";")
		}.toMutableList().apply { this.removeLast() }
	}
}