import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

object MainDev extends App {
  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = ExecutionContext.global

  val minioRepository = MinioRepository.fromDefaultValues()


//  val xd = Await.result(
//    minioRepository.presignedURL("test", "test.png").runWith(Sink.head),
//    Duration(1, TimeUnit.MINUTES)
//  )

  val xd = Await.result(
    minioRepository.listBucketContent("test").runWith(Sink.head),
    Duration(1, TimeUnit.MINUTES)
  )

  println("start")
  println(xd)
  println("end")
}
