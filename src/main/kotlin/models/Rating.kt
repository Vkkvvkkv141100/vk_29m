package models
import getStudents
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.litote.kmongo.Id
import org.litote.kmongo.newId

@Serializable
data class Rating(
	override val name: String, // Наименование
	val groupId: Pair<@Contextual Id<Group>,Int>, //index to id
	@Contextual
	val id : Id<Rating> = newId()
):Named {
var values: MutableList<StudentRating> = mutableListOf<StudentRating>() //student to value
	override fun toString(): String {
		return "${this.groupId.first};${this.groupId.second};${this.id};${this.name};${this.values.map { it.toJson() }}"
	}
//	fun toJson(): String {
//		return "{\"name\":${this.name},\"subGroups\":${this.subGroups.map { it.toJson() }},\"id\":${this.id}}"
//	}

	fun createGroupRating(tasks: List<Task>, group: List<Group>) { // Создание рейтинга группы
		val studentsIdInGroup = getStudents(group, groupId.second).map { it.id }.toList() // Все студенты в группе
		tasks.forEach { task ->
			task.results.filter { it.studentId in studentsIdInGroup }.forEach { result ->//////////////
				if (result.studentName in values.map{it.studentName})
					values.add(StudentRating(result.studentName,
						values.find { it.studentName == result.studentName }?.studentValue?.plus((result.mark * task.weight))!!.toFloat()
					,this.name/*,result.studentId*/))
				else
					values.add(StudentRating(result.studentName,  result.mark * task.weight,name/*, result.studentId*/))
			}
		}
	}

	fun refreshGroupRating(tasks: List<Task>, group: List<Group>): MutableList<StudentRating> {
		this.values.clear()
		createGroupRating(tasks, group)
		return this.values
	}
}