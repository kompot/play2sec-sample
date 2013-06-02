/*
 * Copyright (c) 2013
 */

package model

import scala.concurrent.ExecutionContext.Implicits.global
import reactivemongo.bson.{BSONObjectID, BSONDocument}
import concurrent.Future
import org.joda.time.DateTime
import TokenType._
import org.mindrot.jbcrypt.BCrypt
import com.github.kompot.play2sec.authentication.user.AuthUser
import MongoConversions._
import play.api.libs.json.JsObject

class TokenService(userService: UserService)
    extends MongoService[Token, tokenReader.type, tokenWriter.type] {
  protected def collectionName = "token"
  implicit protected def bsonDocumentReader = tokenReader
  implicit protected def bsonDocumentWriter = tokenWriter

  def generateToken(user: AuthUser, tokenType: TokenType.Value, data: JsObject): Future[Token] = {
    generateToken(tokenType, DateTime.now.plusDays(1), user, data)
  }

  private def generateToken(tokenType: TokenType.Value, expiresAt: DateTime,
      user: AuthUser, data: JsObject): Future[Token] = {
    for (
      user <- userService.getByAuthUserIdentity(user)
    ) yield {
      user.map { u =>
        val token = Token(BSONObjectID.generate, u._id, generateSecurityKey(),
          DateTime.now, expiresAt, ValueSet(tokenType), data)
        insert(token)
        token
      } getOrElse {
        throw new IllegalArgumentException(s"User not found by AuthUser $user")
      }
    }
  }

  def generateSecurityKey() = BCrypt.gensalt().replaceAll("[^A-Za-z0-9]", "")

  // TODO: expiration time is not used
  def getValidTokenBySecurityKey(s: String): Future[Option[Token]] =
    queryOne(BSONDocument("securityKey" -> s))
}
