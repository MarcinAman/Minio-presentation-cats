import java.io.IOException
import java.net.URL

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import akka.stream.scaladsl.Source

import scala.concurrent.Future

object HttpUtils {

  def downloadPicture[ID, A](catsPicture: CatsPicture[ID, A])
                            (implicit actorSystem: ActorSystem): Source[DownloadedCatsPicture[ID, A], NotUsed] = {
    Source.single(
      DownloadedCatsPicture(
        picture = new URL(catsPicture.url).openStream(),
        ref = catsPicture
      )
    )
  }

  def getRequest(url: String)(implicit as: ActorSystem, materializer: Materializer): Source[String, NotUsed] = {
    Source
      .fromFuture(Http().singleRequest(HttpRequest(uri = url)))
      .flatMapConcat(responseBody)
  }

  private def responseBody(httpResponse: HttpResponse)(implicit m: Materializer): Source[String, NotUsed] = {
    Source.fromFuture(
      httpResponse match {
        case HttpResponse(StatusCodes.OK, _, entity, _) => Unmarshal(entity).to[String]
        case HttpResponse(status, _, entity, _) =>
          entity.discardBytes()
          Future.failed(new IOException(s"API returned code $status"))
      }
    )
  }
}
