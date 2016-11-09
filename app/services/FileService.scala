package services

import javax.inject.{Inject, Singleton}
import play.Configuration

trait FileService {
  def getFilePath(): String
}

@Singleton
class ConfigFileService @Inject()(config: Configuration) extends FileService {
  override def getFilePath(): String = config.getString("tangerine-clinic.file")
}