import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext
import scala.io.StdIn

object Main extends App {
  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = ExecutionContext.global

  val minioRepository = MinioRepository.fromDefaultValues()
  val bucketName = ConfigFactory.load().getString("minio.bucket.name")

  val initialFilesUpload: Source[FileLocation, NotUsed] = CatsPicture.initialFiles().flatMapConcat(
    picture => minioRepository.uploadPicture(
      file = picture.picture,
      fileLocation = FileLocation(bucketName, picture.fileName),
      contentType = picture.contentType
    )
  )

  val setup = for {
    c <- minioRepository.createBucket(bucketName).runWith(Sink.ignore)
    u <- initialFilesUpload.runWith(Sink.ignore)
  } yield (c,u)

  val route: Route =
    pathSingleSlash {
      get {
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, HttpUtils.handleRandomPictureRequest()))
      }
    }

  val bindingFuture = setup.flatMap(_ => Http().bindAndHandle(route, "localhost", 8080))

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine()
  bindingFuture
    .flatMap(_.unbind())
    .onComplete(_ => system.terminate())
}