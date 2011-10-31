package processors

import util.Logging
import models.dos.{Task}
import play.Play

/**
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */

trait Processor extends Logging {

  /**
   * Does its thing given a path and optional parameters. The path may or may not exist on the file system.
   */
  def process(task: Task, processorParams: Map[String, AnyRef] = Map.empty[String, AnyRef])

  def isImage(name: String) = name.contains(".") && !name.startsWith(".") && (
          name.split("\\.")(1).toLowerCase match {
            case "jpg" | "tif" | "tiff" => true
            case _ => false
          })

  def getGMCommand(task: Task): Option[String] = {
    // this is needed because OS X won't run commands unless given the full path
    val gmCommand = Play.configuration.getProperty("dos.graphicsmagic.cmd")
    if(gmCommand == null) {
      error(task, "Could not find path to GraphicsMagick in application.conf under key 'dos.graphicsmagic.cmd'")
      None
    } else Some(gmCommand)
  }

}