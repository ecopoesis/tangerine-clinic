package controllers

import javax.inject._

import play.api._
import play.api.mvc._
import services.{FileService, LineReader}

@Singleton
class LineController @Inject()(lineReader: LineReader) extends Controller {

  def getLine(n: Long) = Action {
    if (n <= 0) {
      BadRequest("invalid line number")
    } else {
      lineReader.getLine(n).map(l => Ok(l)).getOrElse(Status(REQUEST_ENTITY_TOO_LARGE))
    }
  }
}
