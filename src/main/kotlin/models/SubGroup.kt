package models

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.litote.kmongo.Id
import org.litote.kmongo.newId

@Serializable
data class SubGroup(
	override val name: String, // Имя подгруппы
	val students: List<Student>, // Список студентов подгруппы
	@Contextual val id: Id<SubGroup> = newId() //id который будет в бд
) : Named {
	//	override fun toString(): String {
//		return "{${this.name};${this.students};${this.id}}"
//	}
//	val csvHeader = "name;students;id"
	fun toJson(): String {
		return "{\"name\":\"${this.name}\",\"students\":${this.students.map { it.toJson() }}}"
	}
}

