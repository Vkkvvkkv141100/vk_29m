package models

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.litote.kmongo.Id
import org.litote.kmongo.newId

@Serializable
data class Student(
	override val name: String,
	@Contextual val id: Id<Student> = newId() //id который будет в бд
) : Named {
	//	override fun toString(): String {
//		return "{${this.name};${this.id}}"
//	}
//	val csvHeader = "name;id"
	fun toJson(): String {
		return "{\"name\":\"${this.name}\",\"id\":\"${this.id}\"}"
	}
}