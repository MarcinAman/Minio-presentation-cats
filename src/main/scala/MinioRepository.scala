import java.io.InputStream

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.typesafe.config.ConfigFactory
import io.minio.MinioClient

case class FileLocation(bucketName: String, fileName: String)

case class MinioRepository private(minioClient: MinioClient) {

  def createBucket(bucketName: String): Source[Unit, NotUsed] = {
    Source.single{
      minioClient.makeBucket(bucketName)
    }
  }

  def deleteBucket(bucketName: String): Source[Unit, NotUsed] = {
    Source.single{
      minioClient.removeBucket(bucketName)
    }
  }

  def presignedGetURL(fileLocation: FileLocation): Source[String, NotUsed] = {
    Source.single{
      minioClient.presignedGetObject(fileLocation.bucketName, fileLocation.fileName)
    }
  }

  def uploadPicture(fileLocation: FileLocation, file: InputStream, contentType: String): Source[FileLocation, NotUsed] = {
    Source.single {
      minioClient.putObject(fileLocation.bucketName, fileLocation.fileName, file, contentType)
      fileLocation
    }
  }
}

object MinioRepository {
  def fromConnection(connectionProperties: MinioConnectionProperties): MinioRepository = {
    val client = new MinioClient(
      connectionProperties.endpoint,
      connectionProperties.accessKey,
      connectionProperties.secretKey
    )

    MinioRepository(client)
  }

  def fromDefaultValues(): MinioRepository = {
    val properties = MinioConnectionProperties(
      endpoint = ConfigFactory.load().getString("minio.connection.endpoint"),
      accessKey = ConfigFactory.load().getString("minio.connection.login"),
      secretKey = ConfigFactory.load().getString("minio.connection.password"))

    fromConnection(properties)
  }
}
