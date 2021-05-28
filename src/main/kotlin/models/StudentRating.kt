package models

import kotlinx.serialization.Serializable

@Serializable
data class StudentRating(
	val studentName: String,
	var studentValue: Float,
	val ratingName: String
) {
//	override fun toString(): String {
//		return "${this.studentName} ${this.studentValue}"
//	}

	fun toJson(): String {
		return "{\"studentName\":\"${this.studentName}\",\"studentValue\":${this.studentValue},\"ratingName\":\"${this.ratingName}\"}"
	}
}