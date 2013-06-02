/*
 * Copyright (c) 2013
 */

package model

import org.joda.time.DateTime
import scala.Some
import play.api.libs.json._
import play.api.libs.functional._
import reactivemongo.bson._
import play.modules.reactivemongo.json.ImplicitBSONHandlers._

object MongoConversions {
  implicit object TokenTypeBSONWriter extends BSONWriter[model.TokenType.ValueSet, BSONArray] {
    def write(t: model.TokenType.ValueSet): BSONArray = {
      t.foldLeft(BSONArray()) { case (z, usage) => z.add(usage.toString) }
    }
  }

  implicit object TokenTypeBSONReader extends BSONReader[BSONArray, model.TokenType.ValueSet] {
    def read(b: BSONArray): model.TokenType.ValueSet = {
      bsonArrayToEnum(Some(b), model.TokenType)
    }
  }

  implicit object DateTimeBSONWriter extends BSONWriter[DateTime, BSONDateTime] {
    def write(t: DateTime): BSONDateTime = BSONDateTime(t.getMillis)
  }

  implicit object DateTimeBSONReader extends BSONReader[BSONDateTime, DateTime] {
    def read(b: BSONDateTime): DateTime = new DateTime(b.value)
  }

  private def bsonArrayToEnum[T <: scala.Enumeration](source: Option[BSONArray], enum: T): T#ValueSet = {
    val set: Set[enum.type#Value] = source.get.iterator.map { n =>
      enum.withName(n.get._2.asInstanceOf[BSONString].value)
    }.toSet
    val builder = enum.ValueSet.newBuilder
    set.map { elem => builder += elem }
    builder.result()
  }

  implicit val remoteUserReader = Macros.reader[RemoteUser]
  implicit val remoteUserWriter = Macros.writer[RemoteUser]
  implicit val userReader = Macros.reader[User]
  implicit val userWriter = Macros.writer[User]
  implicit val tokenReader = Macros.reader[Token]
  implicit val tokenWriter = Macros.writer[Token]
}
