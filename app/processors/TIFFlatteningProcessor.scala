package processors

import models.dos.Task
import org.im4java.process.OutputConsumer
import play.Play
import org.im4java.core.{ImageCommand, IMOperation}
import java.io.{File, InputStreamReader, BufferedReader, InputStream}

/**
 * This processor "flattens" TIF images that have more than one layer and keeps only the biggest layer, so that
 * tiling works nicely with it. Original images are moved to a new subdirectory called "_original"
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */

object TIFFlatteningProcessor extends Processor {

  val FLATTENED_PREFIX: String = "FLATTENED_"

  def process(task: Task, processorParams: Map[String, AnyRef]) {
    if(!task.pathExists) {
      error(task, "Cannot find directory '%s'".format(task.path))
      return
    }

    // this is needed because OS X won't run commands unless given the full path
    val gmCommand = Play.configuration.getProperty("dos.graphicsmagic.cmd")
    if(gmCommand == null) {
      error(task, "Could not find path to GraphicsMagick in application.conf under key 'dos.graphicsmagic.cmd'")
      return
    }

    val oldDir = new File(task.pathAsFile, "_original")
    oldDir.mkdir()


    val images = task.pathAsFile.listFiles().filter(f => isImage(f.getName))

    for (i <- images; if (!task.isCancelled)) {
      val identifiyCmd = new ImageCommand(gmCommand, "identify")
      val identifyOp: IMOperation = new IMOperation
      identifyOp.addImage(i.getAbsolutePath)
      var identified: List[String] = List()
      identifiyCmd.setOutputConsumer(new OutputConsumer() {
        def consumeOutput(is: InputStream) {
          val br = new BufferedReader(new InputStreamReader(is))
          identified = Stream.continually(br.readLine()).takeWhile(_ != null).toList
        }
      })
      identifiyCmd.run(identifyOp)

      if(identified.length > 1) {
        // gm identify gives us lines like this:
        // 2006-011.tif TIFF 1000x800+0+0 DirectClass 8-bit 3.6M 0.000u 0:01
        // we want to fetch the 1000x800 part and know which line is da biggest
        val largestLayer =
          (identified.map(line => {
            val Array(width: Int, height: Int) = line.split(" ")(2).split("\\+")(0).split("x").map(Integer.parseInt(_))
            (width, height)
          })
            .zipWithIndex
            .foldLeft((0, 0), 0) {
              (r: ((Int, Int), Int), c: ((Int, Int), Int)) => if(c._1._1 * c._1._2 > r._1._1 * r._1._2) c else r
             })

        val largestIndex = largestLayer._2

        info(task, "Found %s layers in image '%s', biggest layer at index %s with size %sx%s, attempting to flatten it".format(identified.length, i.getAbsolutePath, largestIndex, largestLayer._1._1, largestLayer._1._2))

        // gm convert nonFlat.tif[0] flat.tif
        val convertCmd = new ImageCommand(gmCommand, "convert")
        val convertOp = new IMOperation
        convertOp.addRawArgs(i.getAbsolutePath + "[%s]".format(largestIndex))
        convertOp.addRawArgs(new File(i.getParentFile.getAbsoluteFile, FLATTENED_PREFIX + i.getName).getAbsolutePath)
        convertCmd.run(convertOp)

        val flattened = new File(i.getParentFile, FLATTENED_PREFIX + i.getName)
        if(flattened.exists()) {
          i.renameTo(new File(oldDir, i.getName))
          flattened.renameTo(new File(oldDir.getParentFile, i.getName))
          info(task, """Image flattened succesfully, moved original to directory "_original"""", Some(i.getAbsolutePath), Some(flattened.getAbsolutePath))
        } else {
          error(task, "Failed to convert the multi-layer image '%s' to a single-layer image".format(i.getAbsolutePath), Some(i.getAbsolutePath))
        }
      }
    }

  }
}