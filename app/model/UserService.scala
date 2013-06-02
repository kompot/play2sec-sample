/*
 * Copyright (c) 2013
 */

package model

import com.github.kompot.play2sec.authentication.user.{ExtendedIdentity,
AuthUserIdentity, AuthUser}
import scala.concurrent.Future
import com.github.kompot.play2sec.authentication.providers.password
.{UsernamePasswordAuthProvider, UsernamePasswordAuthUser}
import play.api.mvc.Request
import com.github.kompot.play2sec.authentication.providers
.MyUsernamePasswordAuthUser
import scala.Some
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import model.MongoConversions._
import scala.concurrent.ExecutionContext.Implicits.global

class UserService extends com.github.kompot.play2sec.authentication.service.UserService
    with MongoService[User, userReader.type, userWriter.type]{
  protected def collectionName = "user"
  protected def bsonDocumentReader = userReader
  protected def bsonDocumentWriter = userWriter

  def getByUsernameOrId(usernameOrId: String): Future[Option[User]] =
    if (isUsernameOrIdIsId(usernameOrId))
      get(BSONObjectID(usernameOrId))
    else
      queryOne(BSONDocument("username" -> usernameOrId))

  private def isUsernameOrIdIsId(usernameOrId: String): Boolean =
    "[A-Fa-f0-9]{24}".r.findFirstIn(usernameOrId).isDefined

  def getByAuthUserIdentity(authUser: Option[AuthUserIdentity]): Future[Option[User]] = {
    if (authUser.isDefined) getByAuthUserIdentity(authUser.get) else Future[Option[User]](None)
  }

  def getByAuthUserIdentity(authUser: AuthUserIdentity): Future[Option[User]] = {
    queryOne(BSONDocument(
      "remoteUsers.provider" -> authUserProviderToRemoteUserProvider(authUser).toString,
      "remoteUsers.id" -> authUser.getId
    ))
  }

  def existsByAuthUserIdentity(authUser: AuthUser): Future[Boolean] = {
    queryOne(BSONDocument(
      "remoteUsers.provider" -> authUserProviderToRemoteUserProvider(authUser).toString,
      "remoteUsers.id" -> authUser.getId
    )).map(!_.isEmpty)
  }

  // TODO remove from sample
  @deprecated
  def usernameIsUnique(username: String, currentUser: Option[User]): Future[Boolean] = {
    // TODO: check case insensitive
    queryOne(BSONDocument("username" -> username)).map(_.filter(u =>
      currentUser.map(_.username != u.username).getOrElse(false)).isEmpty)
  }

  def checkLoginAndPassword(email: String, password: String): Future[Boolean] = {
    val loginUser = new MyUsernamePasswordAuthUser(password, email)
    for {
      dbu <- getByAuthUserIdentity(loginUser)
    } yield {
      dbu.map{ user => loginUser.checkPassword(user.password, password)}.getOrElse(false)
    }
  }

  def emailExists(email: String): Future[Boolean] =
    existsByAuthUserIdentity(new MyUsernamePasswordAuthUser("", email))

  def createByAuthUserIdentity(authUser: AuthUser): User = {
    val remoteUser = new RemoteUser(authUserProviderToRemoteUserProvider(authUser).toString, authUser.getId)
    val pass = if (authUser.isInstanceOf[UsernamePasswordAuthUser]) authUser.asInstanceOf[UsernamePasswordAuthUser].getHashedPassword else ""
    val nameLast = if (authUser.isInstanceOf[ExtendedIdentity]) authUser.asInstanceOf[ExtendedIdentity].getLastName else ""
    val nameFirst = if (authUser.isInstanceOf[ExtendedIdentity]) authUser.asInstanceOf[ExtendedIdentity].getFirstName else ""

    val user = new User(BSONObjectID.generate, None, Some(pass), nameLast,
      nameFirst, Set(remoteUser))
    // could not afford async - we must be sure that user has been created
    MongoWait(insert(user))
    user
  }

  def authUserProviderToRemoteUserProvider(authUser: AuthUserIdentity): RemoteUserProvider.Value = {
    RemoteUserProvider.withName(authUser.getProvider)
  }

  def showUsers() = query()

  def save(authUser: AuthUser): Option[Any] = {
    if (!MongoWait(existsByAuthUserIdentity(authUser))) {
      Some(createByAuthUserIdentity(authUser))
    } else {
      None
    }
  }

  def getByAuthUserIdentitySync(identity: AuthUserIdentity): Option[User] = {
    MongoWait(getByAuthUserIdentity(identity))
  }

  def merge(newUser: AuthUser, oldUser: Option[AuthUser]) = {
    link(oldUser, newUser)
    newUser
  }

  def link(oldUser: Option[AuthUser], newUser: AuthUser): AuthUser = {
    oldUser match {
      case None => newUser
      case Some(old) => {
        val u = getByAuthUserIdentitySync(old)
        u.map { o =>
          // isConfirmed should be set according to provider, e. g. for email
          // provider we should set it only when clicked on a link in an email
          val newRu = RemoteUser(newUser.getProvider, newUser.getId, isConfirmed = true)
          collection.update(
            BSONDocument("_id" -> u.get._id),
            BSONDocument("$push" ->
                BSONDocument("remoteUsers" -> remoteUserWriter.write(newRu))
            )
          )
        }
        old
      }
    }
  }

  def whenLogin[A](knownUser: AuthUser, request: Request[A]): AuthUser = knownUser

  def verifyEmail(userId: BSONObjectID, email: String): Future[Boolean] = {
    collection.update(
      BSONDocument(
        "_id" -> userId,
        "remoteUsers.provider" -> UsernamePasswordAuthProvider.PROVIDER_KEY,
        "remoteUsers.id" -> email,
        "remoteUsers.isConfirmed" -> false
      ),
      BSONDocument("$set" -> BSONDocument("remoteUsers.$.isConfirmed" -> true)),
      writeConcern = awaitJournalCommit
    ).map{ res =>
      // TODO: return real result, make use of MongoDB updated documents count
      // right now it will return true even when email is forged
      res.ok
    }
  }

  /**
   * Detach account from an existing local user.
   * Returns the auth user to log in with
   *
   * @param currentUser
   * @param provider
   * @return
   */
  def unlink(currentUser: Option[AuthUser], provider: String) = {
    currentUser.map { user =>
      getByAuthUserIdentity(user).map { maybeUser =>
        maybeUser.map { user =>
          collection.update(
            BSONDocument("_id" -> user._id),
            BSONDocument("$pull" ->
                BSONDocument("remoteUsers" ->
                    BSONDocument("provider" -> provider)
                )
            ), awaitJournalCommit
          )
        }
      }
      user
    }
  }
}
