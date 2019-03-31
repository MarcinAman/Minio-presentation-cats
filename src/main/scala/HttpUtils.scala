import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import com.typesafe.config.ConfigFactory

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object HttpUtils {

  def handleRandomPictureRequest()(implicit ac: ActorSystem, m: Materializer): String = {
    val url = downloadUploadAndServeURL()
    s"<h1>Random Cat picture: </h1> <br> <img src=${Await.result(url.runWith(Sink.head), Duration.Inf)}>"
  }

  def downloadUploadAndServeURL()(implicit ac: ActorSystem, m: Materializer): Source[String, NotUsed] = {
    val bucketName = ConfigFactory.load().getString("minio.bucket.name")

    val minioRepository = MinioRepository.fromDefaultValues()

    minioRepository.randomPicturePresignedUrl(bucketName)
  }
}
