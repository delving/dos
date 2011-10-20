package models {

import com.mongodb.casbah.{MongoDB, MongoConnection}
import com.novus.salat.Context
import play.Play
import java.util.Date
import com.novus.salat.dao.SalatDAO
import org.bson.types.ObjectId

package object dos {

  val connection: MongoDB = MongoConnection()("dis")
  val taskCollection = connection("Tasks")

  implicit val ctx = new Context {
    val name = Some("PlaySalatContext")
  }
  ctx.registerClassLoader(Play.classloader)
}

package dos {

import com.mongodb.casbah.commons.MongoDBObject

case class TaskType(name: String)

object TaskType {
  val THUMBNAILS = TaskType("thumbnails")
  val TILES = TaskType("tiles")
  val values = List(THUMBNAILS, TILES)
  def valueOf(what: String) = values find { _.name == what }
}

case class TaskState(name: String)

object TaskState {
  val QUEUED = TaskState("queued")
  val RUNNING = TaskState("runnung")
  val FINISHED = TaskState("finished")
  val CANCELLED = TaskState("cancelled")
  val values = List(QUEUED, RUNNING, FINISHED, CANCELLED)
  def valueOf(what: String) = values find { _.name == what }

}

case class Task(_id: ObjectId = new ObjectId, path: String, taskType: TaskType, queuedAt: Date = new Date, startedAt: Option[Date] = None, finishedAt: Option[Date] = None, state: TaskState = TaskState.QUEUED)

object Task extends SalatDAO[Task, ObjectId](collection = taskCollection) {

  def list(taskType: TaskType) = Task.find(MongoDBObject("taskType.name" -> taskType.name)).toList
  def list(state: TaskState) = Task.find(MongoDBObject("state.name" -> state.name)).toList
  def listAll() = Task.find(MongoDBObject()).sort(MongoDBObject("queuedAt" -> 1)).toList

}

}

}
