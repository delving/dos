package models

import com.mongodb.casbah.{MongoDB, MongoConnection}
import com.novus.salat.Context
import play.Play
import java.util.Date
import com.novus.salat.dao.SalatDAO
import org.bson.types.ObjectId
import modelContext._

package object modelContext {
  val connection: MongoDB = MongoConnection()("dis")
  val taskCollection = connection("Tasks")

  implicit val ctx = new Context {
    val name = Some("PlaySalatContext")
  }
  ctx.registerClassLoader(Play.classloader)
}


case class Task(queued: Date, started: Option[Date] = None, path: String)

object Task extends SalatDAO[Task, ObjectId](collection = taskCollection)