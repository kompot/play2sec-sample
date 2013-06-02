/*
 * Copyright (c) 2013
 */

package model

import reactivemongo.bson.BSONObjectID
import org.joda.time.DateTime
import play.api.libs.json.{JsObject, JsNull, JsValue}

case class Token(_id: BSONObjectID, userId: BSONObjectID,
    securityKey: String, created: DateTime, expires: DateTime,
    usages: TokenType.ValueSet, data: JsObject) extends MongoEntity

object TokenType extends Enumeration {
  val CONFIRM_EMAIL = Value
}
