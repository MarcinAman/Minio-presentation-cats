import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpMethods
import akka.stream.Materializer
import akka.stream.alpakka.s3.scaladsl.S3
import akka.stream.alpakka.s3._
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.typesafe.config.ConfigFactory

case class MinioRepository private(settings: S3Settings) {

  def listBucketContent(bucketName: String): Source[ListBucketResultContents, NotUsed] = {
    S3.listBucket(bucketName, None)
      .withAttributes(S3Attributes.settings(settings))
  }

  def createBucket(bucketName: String): Source[Unit, NotUsed] = {
    S3.request(
      bucket = bucketName,
      key = "",
      method = HttpMethods.PUT
    ).map(_ => Unit)
  }

  def deleteBucket(bucketName: String): Source[Unit, NotUsed] = {
    S3.request(
      bucket = bucketName,
      key = "",
      method = HttpMethods.DELETE
    ).map(_ => Unit)
  }

  def presignedURL(bucketName: String, fileName: String): Source[String, NotUsed] = ???

  def downloadFile(bucketName: String, fileName: String): Source[Option[(Source[ByteString, NotUsed], ObjectMetadata)], NotUsed] = {
    S3.download(bucket = bucketName, key = fileName)
  }

  def uploadFile(bucketName: String, fileName: String, file: Source[ByteString, NotUsed])(implicit m: Materializer): Source[MultipartUploadResult, NotUsed] = {
    file.runWith(S3.multipartUpload(bucket = bucketName, key = fileName))
  }
}

object MinioRepository {
  def fromConnection(connectionProperties: MinioConnectionProperties)(implicit actorSystem: ActorSystem): MinioRepository = {
    val settings = AwsConnectionSettingsProvider.getSettings(connectionProperties)
    MinioRepository(settings)
  }

  def fromDefaultValues()(implicit actorSystem: ActorSystem): MinioRepository = {
    val properties = MinioConnectionProperties(
      endpoint = ConfigFactory.load().getString("minio.connection.endpoint"),
      accessKey = ConfigFactory.load().getString("minio.connection.login"),
      secretKey = ConfigFactory.load().getString("minio.connection.password"))

    fromConnection(properties)
  }
}
