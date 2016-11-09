package models

import play.api.http.Status

sealed trait Errors { def code: Int }

case object FileError extends Errors { val code = Status.NOT_FOUND }
case object InvalidLineNumber extends Errors { val code = Status.BAD_REQUEST }
case object LineNumberToLarge extends Errors { val code = Status.REQUEST_ENTITY_TOO_LARGE }

