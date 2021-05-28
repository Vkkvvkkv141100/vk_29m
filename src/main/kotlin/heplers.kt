import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import models.*
import org.json.JSONObject
import org.litote.kmongo.json
import java.util.*
import org.litote.kmongo.toId

object DateAsLongSerializer : KSerializer<Date> {
	override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.LONG)
	override fun serialize(encoder: Encoder, value: Date) = encoder.encodeLong(value.time)
	override fun deserialize(decoder: Decoder): Date = Date(decoder.decodeLong())
}

fun getFormatList(
	list: List<Named>,
	tabIndex: Int = 0,
	initial: String = "",
	extra: (Int) -> String = { "" }
): String { // Читабельный вывод переданного списка
	return list.foldIndexed(initial) { index, acc, it ->
		acc + "\t".repeat(tabIndex) + (index + 1) + "." + it.name + "\n" + extra(index)
	}
}
fun createStudentList(): List<Student> { // Читабельный вывод списка студентов
	val res = mutableListOf<Student>()
	while (true) {
		val inp = getInputString("Enter student name(0 to break):\n", "Create>Group>SubGroup>Student>name>")
		if (inp == "0") return res
		res.add(Student(inp))
	}
}

fun createSubGroups(): List<SubGroup> { // Создание подгрупп
	val res = mutableListOf<SubGroup>()
	while (true) {
		val inp = getInputString("Enter sub group name(0 to break):\n", "Create>Group>SubGroup>name>")
		if (inp == "0") return res
		res.add(SubGroup(inp, createStudentList()))
	}
}

fun getCreatedGroups(): List<Group> { // Получение созданных групп
	val res = mutableListOf<Group>()
	while (true) {
		val inp = getInputString("Enter group name(0 to break):\n", "Create>Group>name>")
		if (inp == "0") return res
		res.add(Group(inp, createSubGroups()))
	}
}

fun getSaveIndex(index: Int): Int { // получение безопасного индекса
	return if (index < 0)
		0
	else index
}

fun getInputFloat(
	inputMessage: String,
	extra: String = ""
): Float? { // Возвращает введенное пользователем число типа FLOAT
	while (true) {
		print(inputMessage + extra)
		val inp = readLine()!!.toFloatOrNull()
		if (inp != null) return inp
	}
}

fun getStudents(groups: List<Group>, groupIndex: Int? = null): List<Student> { // Возвращает список студентов
	val res = mutableListOf<Student>()
	return if (groupIndex == null) {
		groups.forEach {
			it.subGroups.forEach { subgroup ->
				subgroup.students.forEach { student ->
					res.add(student)
				}
			}
		}
		res
	} else {
		groups[groupIndex].subGroups.map { subgroup ->
			subgroup.students.map { res.add(it) }
		}
		res
	}
}

private fun asd(a: List<StudentRating>) = a

fun getFullRatingOrByGroup(
	ratings: List<Rating>,
	groupIndex: Int = -1,
	extra: (List<StudentRating>) -> List<StudentRating> = ::asd
): List<StudentRating> { // Возвращает список рейтингов студентов группы или полностью в зависимости от переданных аргументов
	if (groupIndex != -1) { // Для одной группы
		return ratings[groupIndex].values
	}
	val res = mutableListOf<StudentRating>() // Для всех групп
	ratings.forEach { rating ->
		rating.values.forEach {
			res.add(it)
		}
	}
	return extra(res)
}

fun getRatingByIndex(
	ratings: List<Rating>,
	ratingIndex: Int = -1
): List<StudentRating> = ratings[ratingIndex].values // Возвращает рейтинг по индексу

fun formatStudentRating(list: List<StudentRating>): String = list.foldIndexed("") { index, acc, it -> // Возвращает читабельную строку списка студентов
	acc + "\t" + (index + 1) + "." + it.studentName + " " + it.ratingName + " " + it.studentValue + "\n"
}

fun getInputString(inputMessage: String, extra: String = ""): String { // Возвращает введенное пользователем значение типа STRING
	print(inputMessage + extra)
	return readLine()!!
}

fun getInputInt(inputMessage: String, extra: String = ""): Int { // Возвращает введенное пользователем значение типа INT
	while (true) {
		print(inputMessage + extra)
		val input = readLine()!!.toIntOrNull()
		if (input != null) return input
	}
}

fun getResults(groups: List<Group>, subject: String = "", initialFill: Boolean = false): List<Result> { // Возвращает список результатов
	val res = mutableListOf<Result>()
	getStudents(groups).forEachIndexed { index, it ->
		val mark = if (initialFill) (0..5).random() else getInputInt("Enter mark to")
		val comment = if (initialFill) "comment_$index" else getInputString("Enter comment")
		res.add(
			Result(
				it.id,
				it.name,
				comment,
				mark
			)
		)
	}
	return res
}

fun <T> reduceToCSV(csvHeader: String, collection: List<T>): String = // Приводит переданную коллекцию в CSV
	csvHeader + "\n" + collection.fold("") { acc, it ->
		acc + it + "\n"
	}

fun parseStudents(studentsSTR: String): List<Student> = studentsSTR.split("},").map { // Парсинг студентов из переданной строки
	Student(
		it.substringAfter("name\":\"").substringBefore("\",\"id\""),
		it.substringAfter("\"id\":\"").substringBefore("\"").toId()
	)
}

fun parseSubGroups(subGroupsSTR: String): List<SubGroup> = // Парсинг групп из переданной строки
	subGroupsSTR.slice(1..subGroupsSTR.lastIndex).split("}]},").toMutableList().apply { this.removeLast() }
		.map { "$it}]}\n" }.map {
			SubGroup(
				it.substringAfter("\"name\":\"").substringBefore("\",\"students\""),
				parseStudents(it.substringAfter("\"students\":[").substringBeforeLast("]"))
			)
		}


fun parseGroupsCSV(stringCSV: String): List<Group> { // Парсинг групп из переданной строки
	val csvInfo = CSV(stringCSV)

	return csvInfo.splitedInfo.map { groupString ->
		Group(
			groupString[csvInfo.header.indexOf("name")],
			parseSubGroups(groupString[csvInfo.header.indexOf("subGroups")]),
			groupString[csvInfo.header.indexOf("id")].toId()
		)
	}
}

fun parseResults(resultSTR: String): List<Result> { // Парсинг результатов из переданной строки
	return resultSTR.split("}, ").map { "$it}" }.map {
		Result(
			it.substringAfter("studentId\":\"").substringBefore("\"").toId(),
			it.substringAfter("studentName\":\"").substringBefore("\""),
			it.substringAfter("comments\":\"").substringBefore("\""),
			it.substringAfter("mark\":").substringBefore(",").toInt(),
			null
		)
	}
}

fun parseTasksCSV(stringCSV: String): List<Task> {// Парсинг заданий из переданной строки
	val csvInfo = CSV(stringCSV)
	return csvInfo.splitedInfo.map { taskString ->
		Task(
			taskString[csvInfo.header.indexOf("name")],
			taskString[csvInfo.header.indexOf("type")],
			taskString[csvInfo.header.indexOf("maxMark")].toIntOrNull() ?: -1,
			taskString[csvInfo.header.indexOf("description")],
			taskString[csvInfo.header.indexOf("subject")],
			parseResults(taskString[csvInfo.header.indexOf("results")]),
			taskString[csvInfo.header.indexOf("weight")].toFloat(),
			null
		)
	}
}

fun parseValues(stringCSV: String): MutableList<StudentRating> = stringCSV.split("}, ").map { "$it}" }.map { // Парсинг рейтингов студентов из переданной строки
	StudentRating(
		it.substringAfter("studentName\":\"").substringBefore("\""),
		it.substringAfter("studentValue\":").substringBefore(",").toFloat(),
		it.substringAfter("ratingName\":\"").substringBefore("\"")
	)
}.toMutableList()

fun parseRatingsCSV(stringCSV: String): List<Rating> { // Парсинг рейтингов студентов из переданной строки
	val csvInfo = CSV(stringCSV)
	return csvInfo.splitedInfo.map { taskString ->
		Rating(
			taskString[csvInfo.header.indexOf("name")],
			Pair(
				taskString[csvInfo.header.indexOf("groupId.first")].toId(),
				taskString[csvInfo.header.indexOf("groupId.second")].toInt()
			),
			taskString[csvInfo.header.indexOf("id")].toId()
		).apply { this.values = parseValues(taskString[csvInfo.header.indexOf("values")]) }
	}
}