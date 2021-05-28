import com.mongodb.client.MongoCollection
import models.*
import org.litote.kmongo.eq
import org.litote.kmongo.find
import org.litote.kmongo.setValue
import org.litote.kmongo.*
import java.io.File

class Cli(
	val groups: MongoCollection<Group>,
	val tasks: MongoCollection<Task>,
	val ratings: MongoCollection<Rating>,
) { // Экземпляр класса для работы с командной строкой
	var input: Int? = null
	val greeting = "1.Create\n2.Read\n3.Update\n4.Delete\n5.Import CSV\n6.Export CSV\n>"

	fun getLastInputInt( // получение последнего введенного пользователем значения типа INT
		inputMessage: String,
		extra: String = "",
		condition: () -> Boolean = { this.input != 0 }
	): Int? {
		while (true) {
			print(inputMessage + extra)
			this.input = readLine()!!.toIntOrNull()
			if (this.input != null || condition()) return this.input
		}
	}

	fun selectTasks(/*groupsDb: List<Group>, groupId: Int*/): List<Task> { // позволяет пользователю выбрать задания при создании рейтинга
		val tasksDb = this.tasks.find().toList()    ////////////////////
		val res = getInputString("Select tasks (1,2,...,n)\n${getFormatList(tasksDb, 0, "")}", "Create>Rating>Task>")
		val selectedTasks = res.split(",").map { it.toInt() - 1 }.map {
			tasksDb[it]
		}
		return selectedTasks
	}

	fun getCreatedRatings(): List<Rating> { // создание рейтинга
		val res = mutableListOf<Rating>()
		while (true) {
			val groupsDb = this.groups.find().toList()
			val inpGroup = getInputString(
				"\"Enter group (0 to break):\n${getFormatList(groupsDb, 0, "")}",
				"Create>Rating>Groups>"
			).toIntOrNull() ?: 0
			val selectedGroupId = groupsDb[getSaveIndex(inpGroup - 1)].id
			val inp = getInputString("Enter rating name(0 to break):\n", "Create>Rating>name>")
			if (inp == "0" || inpGroup - 1 < 0) return res
			val temp = Rating(inp, selectedGroupId to inpGroup - 1)
			temp.createGroupRating(selectTasks(/*groupsDb, inpGroup - 1*/), groupsDb)
			res.add(temp)
		}
	}
	fun start() {
		while (true) {
			getLastInputInt(greeting)
			when (this.input) {
				1 -> {
					getLastInputInt("1.Create Groups\n2.Create Ratings\n", extra = "Create>")
					when (this.input) {
						1 -> groups.insertMany(getCreatedGroups())
						2 -> ratings.insertMany(getCreatedRatings())
					}
				}
				2 -> {
					getLastInputInt("1.Read Groups\n2.Read Tasks\n3.Read Students\n4.Read Ratings\n", extra = "Read>")
					when (this.input) { // 2
						1 -> { // Groups +
							val groupsDb = this.groups.find().toList()
							getLastInputInt(getFormatList(groupsDb, 0, ""), "Read>Groups>")

							when (this.input) {
								in 1..groupsDb.size -> { //Groups>Subgroups
									getLastInputInt("Id:${groupsDb[this.input!! - 1].id}\n" +
											"Group name:${groupsDb[this.input!! - 1].name}\n" +
											"Subgroups:\n${
												getFormatList(groupsDb[this.input!! - 1].subGroups, 1) {
													getFormatList(
														groupsDb[this.input!! - 1].subGroups[it].students,
														2
													)
												}
											}"
									)
									this.input = 2147483647
								}
							}
						}
						2 -> { // Tasks
							val tasksDb = this.tasks.find().toList()
							getLastInputInt(getFormatList(tasksDb, 0, ""), "Read>Tasks>")
							when (this.input) {
								in 1..tasksDb.size -> { //Tasks>Results
									getLastInputInt("name:${tasksDb[this.input!! - 1].name}\n" +
											"Subject name:${tasksDb[this.input!! - 1].subject}\n" +
											"Type:${tasksDb[this.input!! - 1].type}\n" +
											"Max mark:${tasksDb[this.input!! - 1].maxMark}\n" +
											"Task name:${tasksDb[this.input!! - 1].weight}\n" +
											"Description:${tasksDb[this.input!! - 1].description}\n" +
											"Date:${tasksDb[this.input!! - 1].date}\n" +
											"Results:\n${
												tasksDb[this.input!! - 1].results.foldIndexed("") { index, acc, it ->
													acc + "\t".repeat(1) + (index + 1) + "." + it.studentName + " " + it.mark + " " + it.comments + " " + it.date + "\n"
												}
											}"
									)
									this.input = 2147483647
								}
							}
						}
						3 -> { // Students
							val studentsDb = getStudents(this.groups.find().toList())
							getLastInputInt(
								"1.All students\n2.Students by rating\n3.Students by group\n",
								"Read>Students>"
							)
							when (this.input) {
								1 -> {//Students>All
									print(getFormatList(studentsDb, 1, ""))
								}
								2 -> {//Students>Rating
									getLastInputInt(
										"1.Students with rating equal X\n2.Students with rating more X\n3.Students with rating less X\n",
										"Read>Students>Rating>X>"
									)
									when (this.input) {
										1 -> printSelectedStudents("eq") { stud, x -> stud == x }
										2 -> printSelectedStudents("gt") { stud, x -> stud > x }
										3 -> printSelectedStudents("lt") { stud, x -> stud < x }
										else -> getLastInputInt("Repeat the input\n", extra = "Read>Students>X>")
									}
								}
								3 -> {//Students>Group
									val groupsDb = this.groups.find().toList()
									getLastInputInt(getFormatList(groupsDb, 0, ""), "Students>Group>")
									print("${getFormatList(getStudents(groupsDb, this.input!! - 1))}\n")
								}
							}
						}
						4 -> { // Ratings
							getLastInputInt(
								"1.All ratings\n2.Ratings for student\n3.Ratings for group\n",
								"Read>Ratings>"
							)
							when (this.input) {
								1 -> {//Ratings>All
									print(
										getFormatRating(
											getRatingByIndex(
												this.ratings.find().toList(),
												getLastInputInt(
													getFormatList(
														this.ratings.find("{},{name:-1, _id:0}").toList(),
														0
													),
													"\nRatings>All>"
												)!! - 1
											)
										)
									)
								}
								2 -> {//Ratings>student
									val studentName = getInputString("Enter student name:\n", "Ratings>Student>")
									print(
										"${
											formatStudentRating(
												getFullRatingOrByGroup(
													this.ratings.find().toList()
												).filter { it.studentName == studentName })
										}\n"
									)
								}
								3 -> {//Ratings>Group
									val groupsDb = this.groups.find().toList()
									getLastInputInt(getFormatList(groupsDb, 0, ""), "Ratings>Group>")
									print(
										"${
											getFormatRating(
												getFullRatingOrByGroup(
													this.ratings.find().toList(),
													this.input!! - 1
												)
											)
										}\n"
									)
								}
							}
						}
						2147483647 -> continue
						else -> getLastInputInt("Repeat the input\n", extra = "Read>")
					}
				}
				3 -> { // Update
					getLastInputInt(
						"1.Update Groups\n2.Update Students\n",
						extra = "Read>"
					)
					when (this.input) {
						1 -> { // Group
							val groupsDb = this.groups.find().toList() /////////
							getLastInputInt(getFormatList(groupsDb, 0, ""), "Update>Groups>")
							val selectedId = groupsDb[this.input!! - 1].id
							val newName = getInputString("Enter new group name:", "\nUpdate>Groups>name>")
							groups.updateOne(
								(Group::id eq selectedId),
								setValue(Group::name, newName)
							)
//							print(groups.find().toList())
						}
						2 -> { // Student
							getLastInputInt(
								"1.Add student\n2.Edit student\n3.Delete student\n",
								extra = "Update>Student>"
							)
							when (this.input) {
								1 -> { // Add
									val groupsDb = this.groups.find().toList() /////////
									val groupIndex =
										getLastInputInt(getFormatList(groupsDb, 0, ""), "Update>Group>Student>")
									val groupId = groupsDb[groupIndex!! - 1].id
									val subgroupIndex =
										getLastInputInt(/*getFormatList(groupsDb, 0, "")*/"Subgroups:\n${
											getFormatList(groupsDb[groupIndex!! - 1].subGroups, 1)
										}", "Update>Group>Student>"
										)
									val subId = groupsDb[groupIndex!! - 1].subGroups[subgroupIndex!! - 1].id
									val newName = getInputString("Enter student name:", "Update>Group>Student>")

									groups.updateOne(
										and((Group::id eq groupId), (Group::subGroups / SubGroup::id eq subId)),
										push(Group::subGroups.posOp / SubGroup::students, Student(newName))
									)
									val rating = ratings.find().toList().find { it.groupId.first == groupId }
									if (rating != null)
										rating.refreshGroupRating(tasks.find().toList(), groups.find().toList())
									else
										println("group dont have rating")
								}
								2 -> { // Edit
									val groupsDb = this.groups.find().toList() /////////
									val groupIndex =
										getLastInputInt(getFormatList(groupsDb, 0, ""), "Update>Group>Student>")
									val groupId = groupsDb[groupIndex!! - 1].id
									val subgroupIndex = getLastInputInt(
										getFormatList(groupsDb, 0, ""), "Subgroups:\n${
											getFormatList(groupsDb[groupIndex!! - 1].subGroups, 1)
										}\n\"Update>Group>Student>"
									)!!

									val studentIndex = getLastInputInt(
										getFormatList(groupsDb, 0, ""), "Students:\n${
											getFormatList(
												groupsDb[groupIndex!! - 1].subGroups[subgroupIndex - 1].students,
												2
											)
										}\n\"Update>Group>Student>"
									)!!
									val studentId =
										groupsDb[groupIndex!! - 1].subGroups[subgroupIndex - 1].students[studentIndex - 1].id

									val newName = getInputString("Enter student name:", "Update>Group>Student>")
									groups.updateMany(
										"{'id':ObjectId('$groupId')}",
										"{'\$set':{'subGroups.${subgroupIndex - 1}.students.${studentIndex - 1}.name': '$newName'}}"
									)
									this.tasks.updateMany(
										"{'results.studentId':ObjectId('$studentId')}",
										"{\$set: {'results.\$.studentName':'$newName'}}}"
									)
									if (ratings.find("{ 'groupId.first':ObjectId('$groupId') }").toList().size != 0) {
										this.ratings.updateMany(
											"{'groupId.first':ObjectId('$groupId')}",
											"{'\$set':{'values':[]}}"
										)
										this.ratings.updateMany(
											"{'groupId.first':ObjectId('$groupId')}", "{'\$set':{'values':${
												stringifyValues(
													this.ratings.find().toList().find { it.groupId.first == groupId }!!
														.refreshGroupRating(
															this.tasks.find().toList(),
															this.groups.find().toList()
														)
												)
											}}}"
										)
									} else {
										println("Group dont have rating")
									}
//									ratings.find().toList().find{it.groupId.first == groupId}!!.refreshGroupRating(tasks.find().toList(), groups.find().toList())
								}
								3 -> { // Delete
									val groupsDb = this.groups.find().toList() /////////
									val groupIndex =
										getLastInputInt(getFormatList(groupsDb, 0, ""), "Update>Group>Student>")
									val groupId = groupsDb[groupIndex!! - 1].id

									val subgroupIndex = getLastInputInt(
										getFormatList(groupsDb, 0, ""), "Subgroups:\n${
											getFormatList(groupsDb[groupIndex!! - 1].subGroups, 1)
										}\n\"Update>Group>Student>"
									)!!

									val studentIndex = getLastInputInt(
										getFormatList(groupsDb, 0, ""), "Students:\n${
											getFormatList(
												groupsDb[groupIndex!! - 1].subGroups[subgroupIndex - 1].students,
												2
											)
										}\n\"Update>Group>Student>"
									)!!
									val studentId =
										groupsDb[groupIndex!! - 1].subGroups[subgroupIndex - 1].students[studentIndex - 1].id

									this.groups.updateMany(
										"{'id':ObjectId('$groupId')}",
										"{'\$pull':{'subGroups.${subgroupIndex - 1}.students': {'id':ObjectId('$studentId')}}}"
									)
									if (ratings.find("{ 'groupId.first':ObjectId('$groupId') }").toList().size != 0) {
										this.tasks.updateMany(
											"{}",
											"{\$pull:{'results':{'studentId':ObjectId('$studentId')}}}"
										)

										this.ratings.updateMany(
											"{'groupId.first':ObjectId('$groupId')}",
											"{'\$set':{'values':[]}}"
										)
										this.ratings.updateMany(
											"{'groupId.first':ObjectId('$groupId')}", "{'\$set':{'values':${
												stringifyValues(
													this.ratings.find().toList().find { it.groupId.first == groupId }!!
														.refreshGroupRating(
															this.tasks.find().toList(),
															this.groups.find().toList()
														)
												)
											}}}"

										)
									} else {
										println("Group dont have rating")
									}
								}
							}
						}
					}
				}
				4 -> { // Delete
					getLastInputInt(
						"1.Delete group\n2.Drop tasks\n3.Drop ratings\n4.Drop all\n",
						extra = "Delete>"
					)
					when (this.input) {
						1 -> {
							getLastInputInt(
								"1.Delete group\n2.Drop groups\n",
								extra = "Delete>Group>"
							)
							when (this.input) {
								1 -> {
									val groupsDb = this.groups.find().toList() /////////
									val groupIndex =
										getLastInputInt(getFormatList(groupsDb, 0, ""), "Delete>Group>")
									val groupId = groupsDb[groupIndex!! - 1].id
									this.groups.deleteOne(Group::id eq groupId)
								}
								2 -> this.groups.drop()
							}
						}
						2 -> this.tasks.drop()

						3 -> this.ratings.drop()

						4 -> {
							this.groups.drop()
							this.ratings.drop()
							this.tasks.drop()
						}
					}
				}
				5 -> { // Import CSV
					getLastInputInt("1.Import groups\n2.Import tasks\n3.Import ratings\n", extra = "Extra>")
					when (this.input) {
						1 -> this.groups.insertMany(parseGroupsCSV(File("src/main/resources/mongo_collections/Groups.csv").readText()))
						2 -> this.tasks.insertMany(parseTasksCSV(File("src/main/resources/mongo_collections/Tasks.csv").readText()))
						3 -> this.ratings.insertMany(parseRatingsCSV(File("src/main/resources/mongo_collections/Ratings.csv").readText()))
					}
				}
				6 -> { // Export CSV
					getLastInputInt("1.Export groups\n2.Export tasks\n3.Export ratings\n", extra = "Extra>")
					when (this.input) {
						1 -> File("src/main/resources/mongo_collections/Groups.csv").writeText(
							reduceToCSV(
								"name;subGroups;id",
								this.groups.find().toList()
							)
						)
						2 -> File("src/main/resources/mongo_collections/Tasks.csv").writeText(
							reduceToCSV(
								"date;description;maxMark;name;results;subject;type;weight",
								this.tasks.find().toList()
							)
						)
						3 -> File("src/main/resources/mongo_collections/Ratings.csv").writeText(
							reduceToCSV(
								"groupId.first;groupId.second;id;name;values",
								this.ratings.find().toList()
							)
						)
					}
				}
				7 -> {
					val localGroups = listOf(
						Group(
							"a1",
							listOf(
								SubGroup(
									"a11",
									listOf(Student("Sergey Brin"), Student("Lary Page"), Student("Satoshi Nakamoto"))
								),
								SubGroup(
									"a12",
									listOf(Student("Mark Zuckerberg"), Student("Kevin Systrom"), Student("Rupert Murdoch"))
								),
								SubGroup("third", listOf(Student("Chloe Moretz"), Student("Emily Stone"), Student("Selena Gomez")))
							)
						),
						Group(
							"a2",
							listOf(
								SubGroup(
									"a21",
									listOf(Student("Student 1"), Student("Student 2"), Student("Student 3"))
								),
								SubGroup(
									"a22",
									listOf(Student("Student 4"), Student("Student 5"), Student("Student 6"))
								),
								SubGroup("a23", listOf(Student("Student 7"), Student("Student 8"), Student("Student 9")))
							)
						),
					)
					val localTasks = listOf(
						Task(
							"Differential equations part 1",
							"lecture",
							5,
							"In mathematics, a differential equation is an equation that relates one or more functions and their derivatives.",
							"math",
							getResults(localGroups, "math", true),
							1.5f
						),
						Task(
							"Differential equations part 2",
							"laboratory",
							10,
							"Differential equations first came into existence with the invention of calculus by Newton and Leibniz. In Chapter 2 of his 1671 work Methodus fluxionum et Serierum Infinitarum.",
							"math",
							getResults(localGroups, "math", true),
							3f
						)
					)
					val localRatings = localGroups.mapIndexed { index, group ->
						Rating("RATING_$index", group.id to index).apply {
							this.createGroupRating(
								localTasks,
								localGroups
							)
						}
					}
					this.groups.insertMany(localGroups)
					this.tasks.insertMany(localTasks)
					this.ratings.insertMany(localRatings)

				}
				2147483647 -> getLastInputInt("", extra = ">")
				else -> getLastInputInt("Repeat the input\n", extra = ">")
			}
		}
	}

	private fun printSelectedStudents(postfix: String, compare: (Float, Float) -> Boolean) { // Печать выбранных студентов
		print(formatStudentRating(getFullRatingOrByGroup(this.ratings.find().toList()) {
			val x = getInputFloat(
				"Enter value X\n",
				"Read>Students>Rating>X>$postfix>"
			)!!
			it.filter { raiting ->
				compare(raiting.studentValue, x)
			}
		}))
	}

	fun getFormatRating( // Читабельный вывод рейтинга
		list: List<StudentRating>,
		tabIndex: Int = 0,
		initial: String = "",
		extra: (Int) -> String = { "" }
	): String = list.foldIndexed(initial) { index, acc, it ->
		acc + "\t".repeat(tabIndex) + (index + 1) + "." + it.studentName + " " + it.studentValue + extra(index) + "\n"
	}


	fun stringifyValues(values: List<StudentRating>): String { // преобразование списка экземпляров класса StudentRating в JSON
		fun asd(): String {
			var b = ""
			values.forEachIndexed { index, it ->
				b += "{\"studentName\":\"${it.studentName}\",\"studentValue\":${it.studentValue},\"ratingName\":\"${it.ratingName}\"}${if (index == values.lastIndex) "" else ","}"
			}
			return b
		}
		return "[${asd()}]"
	}
}
