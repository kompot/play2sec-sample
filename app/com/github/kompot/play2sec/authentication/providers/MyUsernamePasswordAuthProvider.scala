/*
 * Copyright (c) 2013
 */

package com.github.kompot.play2sec.authentication.providers

import com.github.kompot.play2sec.authentication.providers.password
.{UsernamePasswordAuthUser, SignupResult, LoginResult,
UsernamePasswordAuthProvider}
import controllers._
import model._
import play.api.Logger
import play.api.Play.current
import play.api.libs.json.{JsObject, JsString, JsValue}
import play.api.mvc.{Results, Request}
import play.api.templates.Html
import play.i18n.Messages
import scala.concurrent.Await
import scala.concurrent.duration._
import com.github.kompot.play2sec.authentication
import controllers.JsResponseError
import controllers.JsResponseOk
import model.User
import model.Token
import controllers.JsResponseError

class MyUsernamePasswordAuthProvider(app: play.Application)
    extends UsernamePasswordAuthProvider[Token, MyLoginUsernamePasswordAuthUser,
        MyUsernamePasswordAuthUser, MyUsernamePasswordAuthUser, (String, String),
        (String, String), String] with JsonWebConversions {
  lazy val tokenService = bootstrap.Global.Injector.tokenService
  protected val mailService = bootstrap.Global.Injector.mailService

  override protected def neededSettingKeys = {
    super.neededSettingKeys ++ List(
      MyUsernamePasswordAuthProvider.SETTING_KEY_VERIFICATION_LINK_SECURE,
      MyUsernamePasswordAuthProvider.SETTING_KEY_PASSWORD_RESET_LINK_SECURE,
      MyUsernamePasswordAuthProvider.SETTING_KEY_LINK_LOGIN_AFTER_PASSWORD_RESET
    )
  }

  protected def buildLoginAuthUser[A](login: (String, String), request: Request[A]) = new MyLoginUsernamePasswordAuthUser(login._2, login._1)

  protected def buildSignupAuthUser[A](signup: (String, String), request: Request[A]) = new MyUsernamePasswordAuthUser(signup._2, signup._1)

  protected def buildResetPasswordAuthUser[A](resetPassword: String, request: Request[A]) = new MyUsernamePasswordAuthUser("", resetPassword)

  protected def getLoginForm = Authorization.loginForm

  protected def getSignupForm = Authorization.signupForm

  protected def getResetPasswordForm = Authorization.resetPasswordForm

  protected def loginUser(authUser: MyLoginUsernamePasswordAuthUser): LoginResult.Value = {
    val u = authentication.getUserService.getByAuthUserIdentitySync(authUser).asInstanceOf[Option[User]]
    if (!u.isDefined) {
      LoginResult.NOT_FOUND
    } else {
      if (!u.get.emailValidated) {
        LoginResult.USER_UNVERIFIED
      } else {
        for (u1 <- u; acc <- u1.remoteUsers) {
          Logger.debug("-- " + acc)
          // TODO: only one email provider available?
          if (getKey.equals(acc.provider)) {
            if (authUser.checkPassword(u1.password, authUser.clearPassword)) {
              // Password was correct
              return LoginResult.USER_LOGGED_IN
            } else {
              // if you don't return here,
              // you would allow the user to have
              // multiple passwords defined
              // usually we don't want this
              Logger.debug("status WRONG_PASSWORD 1")
              return LoginResult.WRONG_PASSWORD
            }
          }
        }
        Logger.debug("status WRONG_PASSWORD 2")
        LoginResult.WRONG_PASSWORD
      }
    }
  }

  protected def signupUser[A](user: MyUsernamePasswordAuthUser, request: Request[A]): SignupResult.Value = {
    val u = authentication.getUserService.getByAuthUserIdentitySync(user).asInstanceOf[Option[User]]
    if (u.isDefined) {
      if (u.get.emailValidated) {
        // This user exists, has its email validated and is active
        SignupResult.USER_EXISTS
      } else {
        // this user exists, is active but has not yet validated its
        // email
        SignupResult.USER_EXISTS_UNVERIFIED
      }
    } else {
      authentication.getUserService.save(user)
      // Usually the email should be verified before allowing login, however
      // if you return SignupResult.USER_CREATED then the user gets logged in directly
      SignupResult.USER_CREATED_UNVERIFIED
    }
  }

  protected def userExists(authUser: UsernamePasswordAuthUser) = routes.Authorization.signup()

  protected def userUnverified(authUser: UsernamePasswordAuthUser) = routes.Authorization.login()

  protected def generateSignupVerificationRecord(user: MyUsernamePasswordAuthUser) = {
    MongoWait(tokenService.generateToken(user, TokenType.CONFIRM_EMAIL,
      JsObject(Seq("email" -> JsString(user.email)))))
  }

  protected def generateResetPasswordVerificationRecord(user: MyUsernamePasswordAuthUser) =
    ???

  protected def getVerifyEmailMailingSubject[A](user: MyUsernamePasswordAuthUser, request: Request[A]) =
    "Play2sec: please verify your email address"

  protected def getVerifyEmailMailingBody[A](vr: Token, user: MyUsernamePasswordAuthUser, request: Request[A]) = {
//    views.html.mail.verifySignup(Html(getVerificationLink(vr, user, request)))(request).body
    // TODO
    "Click on the link to authorize " + getVerificationLink(vr, user, request)
  }

  protected def getResetPasswordEmailMailingSubject[A](user: MyUsernamePasswordAuthUser, request: Request[A]) =
    Messages.get("mail.subject.resetPassword")

  protected def getResetPasswordEmailMailingBody[A](vr: Token, user: MyUsernamePasswordAuthUser, request: Request[A]) = {
//    views.html.mail.passwordReset(Html(getResetPasswordLink(vr, user, request)))(request).body
    // TODO
    "getResetPasswordEmailMailingBody"
  }

  private def getVerificationLink[A](token: Token, user: MyUsernamePasswordAuthUser,
      request: Request[A]): String = {
    routes.Authorization.verifyEmailAndLogin(token.securityKey).absoluteURL()(request)
  }
//
//  private def getResetPasswordLink[A](verificationRecord: Token, user: MyUsernamePasswordAuthUser, request: Request[A]): String = {
//    routes.Authorization.resetPassword(verificationRecord.securityKey).absoluteURL()(request)
//  }

  protected def userExistsJson(authUser: UsernamePasswordAuthUser) =
    Results.BadRequest[JsValue](JsResponseError(
      "User with such credentials already exists."))

  def userSignupUnverifiedJson(user: MyUsernamePasswordAuthUser) =
    Results.Ok[JsValue](
      JsResponseOk("Email has been sent. You must confirm it.")
    )

  def userLoginUnverifiedJson(value: MyLoginUsernamePasswordAuthUser) =
    Results.BadRequest[JsValue](JsResponseError(
      "Email was not verified, please check your mail."))

  protected def onLoginUserNotFoundJson[A](request: Request[A]) =
    Results.BadRequest[JsValue](JsResponseError(
      "Unknown email or password."))

  protected def onSuccessfulRecoverPasswordJson[A]() =
    Results.Ok[JsValue](JsResponseOk(
      "Please follow instructions in the email."))
}

object MyUsernamePasswordAuthProvider {
  val SETTING_KEY_VERIFICATION_LINK_SECURE = UsernamePasswordAuthProvider.SETTING_KEY_MAIL + "." + "verificationLink.secure"
  val SETTING_KEY_PASSWORD_RESET_LINK_SECURE = UsernamePasswordAuthProvider.SETTING_KEY_MAIL + "." + "passwordResetLink.secure"
  val SETTING_KEY_LINK_LOGIN_AFTER_PASSWORD_RESET = "loginAfterPasswordReset"
  // TODO: used?
//  val getProvider = a.getProvider(UsernamePasswordAuthProvider.PROVIDER_KEY).get.asInstanceOf[MyUsernamePasswordAuthProvider]
}
