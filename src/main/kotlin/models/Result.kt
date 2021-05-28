package models

import DateAsLongSerializer
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.litote.kmongo.Id
import java.util.*

@Serializable
data class Result(
	val studentId: @Contextual Id<Student>,// Id студента в бд
	val studentName: String,// Имя студента
	val comments: String? = null, // Комментарий( причечание )
	val mark: Int = 0,// Оценка, по-умолчанию 0
	@Serializable(with = DateAsLongSerializer::class)
	val date: Date? = null // Дата(Если ничег не ввести, то ничего не добавится)
){
	fun toJson(): String {
		return "{\"studentId\":\"${this.studentId}\",\"studentName\":\"${this.studentName}\",\"comments\":\"${this.comments}\",\"mark\":${this.mark},\"date\":${this.date}}"
	}
}