package processors

import controllers.dos._
import models.dos.Task
import java.io.File
import controllers.dos.Thumbnail
import org.bson.types.ObjectId
import com.mongodb.casbah.commons.MongoDBObject

/**
 * 
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */

object ThumbnailDeletionProcessor extends Processor with Thumbnail {
  def process(task: Task, processorParams: Map[String, AnyRef]) {
    val p = new File(task.path)
    if (!p.exists()) {
      error(task, "Path '%s' does not exist or is unreachable".format(task.path))
    } else {
      if(!task.params.contains(controllers.dos.COLLECTION_IDENTIFIER_FIELD) || !task.params.contains(controllers.dos.ORGANIZATION_IDENTIFIER_FIELD)) {
        error(task, "No spec or organisation provided")
      } else {
        info(task, "Starting to delete thumbnails for directory " + task.path)
        val thumbs = fileStore.find(MongoDBObject(ORIGIN_PATH_FIELD -> task.path.r, COLLECTION_IDENTIFIER_FIELD -> task.params(COLLECTION_IDENTIFIER_FIELD).toString, ORGANIZATION_IDENTIFIER_FIELD -> task.params(ORGANIZATION_IDENTIFIER_FIELD).toString))
        thumbs foreach {
          t => {
            val origin = t.get(ORIGIN_PATH_FIELD).toString
            info(task, "Removing thumbnails for image " + origin, Some(origin))
            fileStore.remove(t.getId.asInstanceOf[ObjectId])
          }
        }
      }
    }
  }
}