package models
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.litote.kmongo.Id
import org.litote.kmongo.newId

@Serializable
data class Group(
	override val name: String, // Имя группы
	val subGroups: List<SubGroup>, // Список подгрупп
	@Contextual val id: Id<Group> = newId() // Id группы
) : Named{
	override fun toString(): String {
		return "${this.name};${this.subGroups.map { it.toJson() }};${this.id}"
	}
//	fun toJson(): String {
//		return "{\"name\":${this.name},\"subGroups\":${this.subGroups.map { it.toJson() }},\"id\":${this.id}}"
//	}
}
