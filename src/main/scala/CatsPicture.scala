import java.io.{File, FileInputStream, InputStream}

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.typesafe.config.ConfigFactory


case class CatsPicture[ID](picture: InputStream, id: ID, contentType: String = "image/jpeg"){
  def fileName: String = id.toString
}

object CatsPicture {
  def initialFiles(): Source[CatsPicture[String], NotUsed] = {
    val directoryName = ConfigFactory.load().getString("pictures.directory")

    val dir = new File(directoryName)
    val contentToUpload = FileUtils.listDirectoryContent(dir)

    val content = contentToUpload.map(
      file => CatsPicture(
        picture = new FileInputStream(file),
        id = file.getName))

    Source.fromIterator(() => content.toIterator)
  }
}
