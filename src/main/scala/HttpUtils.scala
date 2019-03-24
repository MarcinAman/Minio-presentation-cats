import java.io.IOException
import java.net.{HttpURLConnection, URL}

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object HttpUtils {

  def downloadPicture[ID, A](catsPicture: CatsPicture[ID, A])
                            (implicit actorSystem: ActorSystem): Source[DownloadedCatsPicture[ID, A], NotUsed] = {
    Source.single{
      val url = new URL(catsPicture.url)
      val httpcon = url.openConnection.asInstanceOf[HttpURLConnection]
      httpcon.addRequestProperty("User-Agent", "Mozilla/4.0")

      DownloadedCatsPicture(
        picture = httpcon.getInputStream,
        ref = catsPicture,
        contentType = httpcon.getContentType
      )
    }
  }

  def getRequest(url: String)(implicit as: ActorSystem, materializer: Materializer): Source[String, NotUsed] = {
    Source
      .fromFuture(Http().singleRequest(HttpRequest(uri = url).withDefaultHeaders().withHeaders(RawHeader("x-api-key","c6deeab8-5ada-4814-a71d-807079034f93"))))
      .flatMapConcat(responseBody)
  }

  def handleRandomPictureRequest()(implicit ac: ActorSystem, m: Materializer): String = {
    val url = downloadUploadAndServeURL()
    s"<h1>Random Cat picture: </h1> <br> <img src=${Await.result(url.runWith(Sink.head), Duration.Inf)}>"
  }

  def downloadUploadAndServeURL()(implicit ac: ActorSystem, m: Materializer): Source[String, NotUsed] = {
    val bucketName = ConfigFactory.load().getString("minio.bucket.name")

    val minioRepository = MinioRepository.fromDefaultValues()
    val catsPicture: Source[CatsPicture[String, Any], NotUsed] = CatsPicture.randomPicture()

    val downloadPicture: Source[DownloadedCatsPicture[String, Any], NotUsed] =
      catsPicture.flatMapConcat(e => HttpUtils.downloadPicture(e))

    val upload = downloadPicture
      .flatMapConcat(picture => {
        minioRepository.uploadPicture(FileLocation(bucketName, picture.ref.id), picture.picture, picture.contentType)
      })

    upload.flatMapConcat{
      fileLocation => minioRepository.presignedGetURL(fileLocation)
    }
  }

  private def responseBody(httpResponse: HttpResponse)(implicit m: Materializer): Source[String, NotUsed] = {
    println(httpResponse)
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
