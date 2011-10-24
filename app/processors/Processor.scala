package processors

import util.Logger
import models.dos.{Task}

/**
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */

trait Processor extends Logger {

  /**
   * Does its thing given a path and optional parameters. The path may or may not exist on the file system.
   */
  def process(task: Task, params: Map[String, AnyRef] = Map.empty[String, AnyRef])

}