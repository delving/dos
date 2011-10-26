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

  def process(task: Task, processorParams: Map[String, AnyRef]) {

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

      val spec = task.params.get("spec").getOrElse({
        error(task, "No spec passed for task " + task)
        return
      })

      val org = task.params.get("org").getOrElse({
        error(task, "No org passed for task " + task)
        return
      })

      val orgPath = new File(tilesOutputBasePath, org)
      if(!orgPath.exists() && !orgPath.mkdir()) {
        error(task, "Could not create tile org path " + orgPath.getAbsolutePath, Some(orgPath.getAbsolutePath))
        return
      }

      // output path = tiles base dir + task org name + task spec name
      val outputPath = new File(orgPath, spec)
      if (outputPath.exists() && outputPath.isDirectory) {
        error(task, "Output directory '%s' already exists, delete it first if you want to re-tile".format(outputPath.getAbsolutePath))
        return
      }
      if (!outputPath.mkdir()) {
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

      for (i <- images; if (!task.isCancelled)) {
        val targetFileName = if (i.getName.indexOf(".") > -1) i.getName.substring(0, i.getName.indexOf(".")) else i.getName
        val targetFile: File = new File(outputPath, targetFileName + ".tif")
        targetFile.createNewFile()

        try {
          val tileInfo = tiler.convert(i, targetFile)
          info(task, "Generated PTIF for file " + i.getName + ": " + tileInfo.getImageWidth + "x" + tileInfo.getImageHeight + ", " + tileInfo.getZoomLevels + " zoom levels", Some(i.getAbsolutePath), Some(targetFile.getAbsolutePath))
        } catch {
          case t => error(task, "Could not create tile for image '%s': %s".format(i.getAbsolutePath, t.getMessage), Some(i.getAbsolutePath))
        }
      }
    }
  }
}