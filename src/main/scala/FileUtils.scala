import java.io.File

object FileUtils {
  def listDirectoryContent(dir: File): Seq[File] = {
    if (dir.exists && dir.isDirectory) {
      dir.listFiles.filter(_.isFile).toSeq
    } else {
      Seq[File]()
    }
  }
}
