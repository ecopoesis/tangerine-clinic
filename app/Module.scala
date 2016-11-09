import com.google.inject.AbstractModule
import services.{ConfigFileService, FileService, IndexedLineReader, LineReader}

class Module extends AbstractModule {

  override def configure() = {
    bind(classOf[FileService]).to(classOf[ConfigFileService])
    bind(classOf[LineReader]).to(classOf[IndexedLineReader]).asEagerSingleton()
  }

}
