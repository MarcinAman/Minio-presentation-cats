import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.MultipartUploadResult
import akka.stream.scaladsl.{Source, StreamConverters}
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext
import scala.io.StdIn

/* zalozenia:
1. Pobieranie obrazkow po wyniku wyszukania google (kotki ^^)
2. Upload kotkow na minio
3. Wyszczegolnienie 1 endpointu z randomowym zdjeciem kota
 */

object Main extends App {
  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = ExecutionContext.global

  def downloadUploadAndServeURL(): Source[String, NotUsed] = {
    val bucketName = ConfigFactory.load().getString("minio.bucket.name")

    val minioRepository = MinioRepository.fromDefaultValues()
    val catsPicture = CatsPicture.randomPicture()

    val downloadPicture: Source[DownloadedCatsPicture[String, Any], NotUsed] =
      catsPicture.flatMapConcat(e => HttpUtils.downloadPicture(e))

    val uploadPicture: Source[MultipartUploadResult, NotUsed] =
      downloadPicture.flatMapConcat(e => {

        minioRepository.uploadFile(
          bucketName = bucketName,
          fileName = e.ref.id,
          file = StreamConverters.fromInputStream(() => e.picture).mapMaterializedValue(_ => NotUsed)
        )
      })

    val url: Source[String, NotUsed] = ???

    url
  }

  val route =
    pathSingleSlash {
      get {
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http</h1>"))
      }
    } ~ path("create") {
      put {
        complete(StatusCodes.OK)
      }
    }

  val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine()
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done
}