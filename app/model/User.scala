/*
 * Copyright (c) 2013
 */

package model

import reactivemongo.bson.BSONObjectID
import com.github.kompot.play2sec.authorization.core.models.{Permission,
Subject}
import com.github.kompot.play2sec.authorization.core.DeadboltRole

case class User(_id: BSONObjectID, username: Option[String], password: Option[String],
    nameLast: String = "", nameFirst: String = "",
    remoteUsers: Set[RemoteUser], isBlocked: Boolean = false,
    roles: Set[String] = Set())
    extends MongoEntity with Subject {

  def emailValidated: Boolean = remoteUsers.filter { ru =>
    ru.provider == RemoteUserProvider.email.toString && ru.isConfirmed
  }.size == 1

  /**
   * Get email if validated. Otherwise [[scala.None]] (if not present
   * or not confirmed).
   * @return
   */
  def email: Option[String] = {
    remoteUsers.filter { ru =>
      ru.provider == RemoteUserProvider.email.toString && ru.isConfirmed
    }.headOption.map(_.id)
  }

  def confirmed: Boolean = !remoteUsers.filter(_.isConfirmed).isEmpty

  def usernameOrId: String = username.getOrElse(pk)

  def getRoles: List[DeadboltRole] = {
    roles.map(DeadboltRole(_)).to[List]
  }

  def getPermissions: List[Permission] = ???

  def pk() = _id.stringify
}
