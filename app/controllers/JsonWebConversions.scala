/*
 * Copyright (c) 2013
 */

package controllers

import play.api.libs.json.{Json, JsString, JsObject, JsValue}
import model.{RemoteUser, User}

case class UserWeb(id: String, usernameOrId: String, username: String, nameLast: String,
    nameFirst: String, remoteUsers: Set[RemoteUserWeb], roles: Set[String]) {
  def this(u: User) = this(u._id.stringify, u.usernameOrId, u.username.getOrElse(""),
    u.nameLast, u.nameFirst, u.remoteUsers.map(new RemoteUserWeb(_)), u.roles)
}

case class RemoteUserWeb(provider: String, id: String, isConfirmed: Boolean) {
  def this(ru: RemoteUser) = this(ru.provider, ru.id, ru.isConfirmed)
}

trait JsonWebConversions {
  implicit val remoteUserWrites = Json.writes[RemoteUserWeb]
  implicit val userWrites = Json.writes[UserWeb]

  implicit def toJson(jro: JsResponse): JsValue = {
    JsObject(Seq(
      ("status" -> JsString(jro.getStatus)),
      ("message" -> JsString(jro.getMessage)),
      ("data" -> jro.getData)
    ))
  }

  implicit def userToJson(user: User): JsValue = {
    Json.toJson(new UserWeb(user))
  }
}
