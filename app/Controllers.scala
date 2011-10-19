package controllers.dos {

import play.mvc.Controller
import java.io.File
import play.mvc.results.Result

object MCP extends Controller {

  def index() = Template

}

object Browser extends Controller with AdditionalActions {

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


}
