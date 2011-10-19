package controllers.dos.ui

import play.mvc.Controller
import play.mvc.results.Result
import java.io.File
import extensions.dos.{DoSJson, Extensions}

/**
 * 
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */

object Browser extends Controller with Extensions {

  def list(data: String): Result = {
    val viewModel = DoSJson.parse[ViewModel](data)

    val f = new File(viewModel.rootPath)
    if (!f.exists()) return Error("Directory '%s' does not exist".format(viewModel.rootPath))
    val directories: Array[File] = f.listFiles.filter(_.isDirectory)
    Json(Map("directories" -> directories.map(d => Directory(d.getAbsolutePath))))
  }
}

case class ViewModel(rootPath: String)

case class Directory(path: String)
