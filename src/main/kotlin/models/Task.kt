package models

import DateAsLongSerializer
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class Task(
	override val name: String, // Наименование
	val type: String, // Тип задания (лекция/лаба/...)
	val maxMark: Int, // Максимальная оценка
	val description: String, // Описание
	val subject: String, // Предмет, которому принадлежит это задание
	val results: List<Result>, // Список результатов выполнения задания
	val weight: Float, // Вес задания
	@Serializable(with = DateAsLongSerializer::class)
	val date: Date? = null
) : Named {
	override fun toString(): String {
		return "${this.date};${this.description};${this.maxMark};${this.name};${this.results.map { it.toJson() }};${this.subject};${this.type};${this.weight}"
	}
}

