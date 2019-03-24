import java.io.InputStream

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods

case class CatsPicture[ID, A](breeds: Seq[A], id: ID, url: String)
case class DownloadedCatsPicture[ID, A](picture: InputStream, ref: CatsPicture[ID, A], contentType: String = "image/jpeg"){
  def fileName: String = {
    extension match {
      case Some(value) => s"${ref.id}.$value"
      case None => ref.id.toString
    }
  }

  private def extension: Option[String] = ref.url.split(".").reverse.headOption
}

object CatsPicture {
  private val apiURL = "https://api.thecatapi.com/v1/images/search"

  def randomPicture()(implicit ac: ActorSystem, m: Materializer): Source[CatsPicture[String, Any], NotUsed] =
    HttpUtils.getRequest(apiURL)
      .map(fromJson)

  def fromJson(json: String): CatsPicture[String, Any] = {
    implicit val formats: DefaultFormats = DefaultFormats

    val parsedJson = JsonMethods.parse(json)(0)
    parsedJson.extract[CatsPicture[String, Any]]
  }
}
