import org.litote.kmongo.*
import org.litote.kmongo.KMongo
import models.*

fun main() {
	val client = KMongo//mongodb://localhost:27017/?readPreference=primary&appname=MongoDB%20Compass&ssl=false
		.createClient("mongodb://localhost:27017/?readPreference=primary&appname=MongoDB%20Compass&ssl=false") // подключение к монго

	val mongoDatabase = client.getDatabase("test") // создание бд
	val groups = mongoDatabase.getCollection<Group>().apply { drop() }
	val tasks = mongoDatabase.getCollection<Task>().apply { drop() }
	val ratings = mongoDatabase.getCollection<Rating>().apply { drop() }
	Cli(
		groups,
		tasks,
		ratings
	).start()
}
