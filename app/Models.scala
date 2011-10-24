package models {

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.{MongoDB, MongoConnection}
import com.novus.salat.Context
import play.Play
import java.util.Date
import com.novus.salat.dao.SalatDAO
import org.bson.types.ObjectId

package object dos {

  val connection: MongoDB = MongoConnection()("dis")
  val taskCollection = connection("Tasks")
  val logCollection = connection("Log")

  implicit val ctx = new Context {
    val name = Some("PlaySalatContext")
  }
  ctx.registerClassLoader(Play.classloader)
}

package dos {

case class Log(_id: ObjectId = new ObjectId,
               task_id: ObjectId,
               date: Date = new Date,
               message: String,
               level: LogLevel = LogLevel.INFO)

object Log extends SalatDAO[Log, ObjectId](collection = logCollection)

case class LogLevel(name: String)
object LogLevel {
  val INFO = LogLevel("info")
  val ERROR = LogLevel("error")
  val values = List(INFO, ERROR)
  def valueOf(what: String) = values find { _.name == what }
}

case class Task(_id: ObjectId = new ObjectId,
                path: String,
                taskType: TaskType,
                queuedAt: Date = new Date,
                startedAt: Option[Date] = None,
                finishedAt: Option[Date] = None,
                state: TaskState = TaskState.QUEUED,
                totalItems: Int = 0,
                processedItems: Int = 0)

object Task extends SalatDAO[Task, ObjectId](collection = taskCollection) {
  def list(taskType: TaskType) = Task.find(MongoDBObject("taskType.name" -> taskType.name)).toList
  def list(state: TaskState) = Task.find(MongoDBObject("state.name" -> state.name)).sort(MongoDBObject("queuedAt" -> 1)).toList
  def listAll() = Task.find(MongoDBObject()).sort(MongoDBObject("queuedAt" -> 1)).toList

  def start(task: Task) {
    Task.update(MongoDBObject("_id" -> task._id), $set ("state.name" -> TaskState.RUNNING.name, "startedAt" -> new Date))
  }

  def finish(task: Task) {
    Task.update(MongoDBObject("_id" -> task._id), $set ("state.name" -> TaskState.FINISHED.name, "finishedAt" -> new Date))
  }

  def setTotalItems(task: Task, total: Int) {
    Task.update(MongoDBObject("_id" -> task._id), $set ("totalItems" -> total))
  }

  def incrementProcessedItems(task: Task, amount: Int) {
    Task.update(MongoDBObject("_id" -> task._id), $inc ("processedItems" -> amount))
  }
}

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
  val RUNNING = TaskState("running")
  val FINISHED = TaskState("finished")
  val CANCELLED = TaskState("cancelled")
  val values = List(QUEUED, RUNNING, FINISHED, CANCELLED)
  def valueOf(what: String) = values find { _.name == what }

}
}

}
