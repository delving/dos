package processors

import at.ait.dme.magicktiler.image.ImageFormat
import at.ait.dme.magicktiler.MagickTiler
import at.ait.dme.magicktiler.ptif.PTIFConverter
import models.dos.Task
import java.io.File
import play.Play

/**
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */

object PTIFTilingProcessor extends Processor {

  def process(task: Task, params: Map[String, AnyRef]) {

    // TODO warn / throw error if these ain't set
    val tilesOutputBasePath = new File(Play.configuration.getProperty("dos.tilesOutputBaseDir", "/tmp/tiles"))
    val tilesWorkingBasePath = new File(Play.configuration.getProperty("dos.tilesWorkingBaseDir", "/tmp/tilesWork"))

    def checkOrCreate(dir: File) = dir.exists() || !dir.exists() && dir.mkdir()

    if (!checkOrCreate(tilesOutputBasePath)) {
      error(task, "Cannot find / create tiles output directory '%s'".format(tilesOutputBasePath.getAbsolutePath))
      return
    }
    if (!checkOrCreate(tilesWorkingBasePath)) {
      error(task, "Cannot find / create tiles working directory '%s'".format(tilesWorkingBasePath.getAbsolutePath))
      return
    }

    val p = new File(task.path)
    if (!p.exists()) {
      error(task, "Path '%s' does not exist or is unreachable".format(task.path))
    } else {

      val outputPath = new File(tilesOutputBasePath, p.getName)
      if(outputPath.exists() && outputPath.isDirectory) {
        error(task, "Output directory '%s' already exists, delete it first if you want to re-tile".format(outputPath.getAbsolutePath))
        return
      }
      if(!outputPath.mkdir()) {
        error(task, "Cannot create output directory '%s'".format(outputPath.getAbsolutePath))
        return
      }

      val tiler: MagickTiler = new PTIFConverter()
      tiler.setWorkingDirectory(tilesWorkingBasePath)
      tiler.setTileFormat(ImageFormat.JPEG);
      tiler.setJPEGCompressionQuality(75);
      tiler.setBackgroundColor("#ffffffff");
      tiler.setGeneratePreviewHTML(false);

      val images = p.listFiles().filter(f => isImage(f.getName))

      images foreach {
        i => {
          info(task, "Making PTIF tile for image '%s'".format(i.getAbsolutePath))

          val targetFileName = if (i.getName.indexOf(".") > -1) i.getName.substring(0, i.getName.indexOf(".")) else i.getName
          val targetFile: File = new File(outputPath, targetFileName + ".tif")
          targetFile.createNewFile()

          try {
            tiler.convert(i, targetFile)
          } catch {
            case t => error(task, "Could not create tile for image '%s': %s".format(i.getAbsolutePath, t.getMessage))
          }
        }
      }
    }
  }
}