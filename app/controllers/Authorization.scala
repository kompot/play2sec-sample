/*
 * Copyright (c) 2013
 */

package controllers

import play.api.mvc._
import com.github.kompot.play2sec.authorization.scala.DeadboltActions
import play.api.libs.json._
import com.github.kompot.play2sec.authentication.providers.password.{Case,
UsernamePasswordAuthProvider}
import com.github.kompot.play2sec.authentication
import com.github.kompot.play2sec.authorization.handler.{UserEditUpdate,
CustomDeadboltHandler}
import com.github.kompot.play2sec.authentication._
import com.github.kompot.play2sec.authentication.user.AuthUser
import play.api.data.Form
import play.api.libs.json.JsString
import play.api.libs.json.JsBoolean
import scala.Some
import play.api.data.Forms._
import play.api.libs.json.JsString
import play.api.libs.json.JsBoolean
import scala.Some
import play.api.libs.json.JsObject
import play.api.mvc.AsyncResult
import play.api.libs.json.JsString
import play.api.libs.json.JsBoolean
import scala.Some
import play.api.libs.json.JsObject
import model.MongoWait
import bootstrap.Global.Injector
import scala.concurrent.ExecutionContext.Implicits.global
import reactivemongo.bson.BSONObjectID
import play.api.Logger
import scala.concurrent.Future

object Authorization extends Controller with DeadboltActions with JsonWebConversions {
  val userService = Injector.userService
  val tokenService = Injector.tokenService

  def auth = Action { implicit request =>
    Ok(views.html.auth.index())
  }

  def login = Action.async { implicit request => userLoginForm.bindFromRequest.fold(
    { errors => Future.successful(BadRequest[JsValue](JsResponseError("Unable to perform login.", Some(errors)))) },
    { case _ => UsernamePasswordAuthProvider.handleLogin(request) }
  ) }

  def signup = Action.async { implicit request =>
    userSignUpForm.bindFromRequest.fold(
    { errors => Future.successful(BadRequest[JsValue](JsResponseError("Unable to perform signup.", Some(errors)))) },
    { case _ => UsernamePasswordAuthProvider.handleSignup(request) }
    )
  }

  def authenticate(provider: String) = Action.async { implicit request =>
    authentication.handleAuthentication(provider, request)
  }

  def requestPasswordReset = Action.async { implicit request =>
    resetPasswordForm.bindFromRequest.fold(
      { errors => Future.successful(BadRequest[JsValue](JsResponseError("Unable to reset password.", Some(errors)))) },
      { case _ => UsernamePasswordAuthProvider.handleRecoverPassword(request) }
    )
  }

  // since method works only for current user, no extra security is required
  // should it be allowed to admins to unlink remote users?
  def unlink(provider: String) = SubjectPresent(new CustomDeadboltHandler()) {
    Action { implicit request =>
      val currentUser = authentication.getUser(request.session)
      Async {
        userService.getByAuthUserIdentity(currentUser).map { user =>
          val prov = user.get.remoteUsers.find(r => r.provider == provider)
          val rup = userService.authUserProviderToRemoteUserProvider(currentUser.get)
          prov.map{ p =>
            if (p.provider == rup.toString) {
              Forbidden[JsValue](JsResponseError(s"Can't unlink provider you are currently logged in with."))
            } else if (user.get.remoteUsers.size == 1) {
              Forbidden[JsValue](JsResponseError(s"Can't unlink last provider to log in with."))
            } else {
              authentication.getUserService.unlink(currentUser, provider)
              Ok[JsValue](
                JsResponseOk(s"Remote provider $provider has been unlinked.",
                  user.get.copy(remoteUsers = user.get.remoteUsers - prov.get)
                )
              )
            }
          }.getOrElse(
            Forbidden[JsValue](JsResponseError(s"Provider $provider not found"))
          )
        }
      }
    }
  }

  def createAnonymousAccount(returnTo: String) = SubjectNotPresent(new CustomDeadboltHandler()) {
    Action.async { implicit request =>
      if (request.session.get(SESSION_ORIGINAL_URL) == None) {
        val redirectTo = request.headers.get(REFERER).orElse(Some("/")).get
        val back = (SESSION_ORIGINAL_URL, if (returnTo != "/") returnTo else redirectTo)
        Future.successful(
          Redirect(routes.Authorization.createAnonymousAccount())
            .withSession(request.session + back)
        )
      } else {
        handleAuthentication("anonymous", request, Some(Case.SIGNUP))
      }
    }
  }

  def logout = Action { implicit request =>
    authentication.logout(request.session)
  }

  def verifyEmailAndLogin(token: String) = Action.async { implicit request =>
    for {
      maybeToken <- tokenService.getValidTokenBySecurityKey(token)
      email = maybeToken.get.data.\("email").as[String]
      res <- userService.verifyEmail(maybeToken.get.userId, email)
      maybeUser <- userService.get(maybeToken.get.userId)
    } yield {
      if (maybeUser.isDefined) {
        if (maybeUser.get.remoteUsers.exists{ r =>
            r.provider == UsernamePasswordAuthProvider.PROVIDER_KEY &&
            r.id == email && r.isConfirmed
        }) {
          val identity = new AuthUser {
            def id = email
            def provider = UsernamePasswordAuthProvider.PROVIDER_KEY
          }
          MongoWait(authentication.loginAndRedirect(request, Future(identity)))
        } else {
          InternalServerError("Email was not verified.")
        }
      } else {
        InternalServerError("Email was not verified.")
      }
    }
  }

  def allowed = Action { implicit request =>
    allowedForm.bindFromRequest().fold(
      errors => Forbidden(s"Not allowed"),
      { case (zone, meta) => {
        AsyncResult {
          for {
            maybeUser <- userService.getByAuthUserIdentity(authentication.getUser(request))
          } yield {
            val allowed = maybeUser.map(u => {
              val adh = new CustomDeadboltHandler()
              adh.getDynamicResourceHandler(request).get.isAllowed(
                zone + "$",
                Form("meta" -> nonEmptyText).bind(Map(("meta", meta))),
                adh, request)
            }).getOrElse(false)
            Ok(JsObject(Seq(
              (zone, JsBoolean(allowed))
            )))
          }
        }
      } }
    )
  }

  val allowedForm = Form(tuple(
    // TODO: replace with real zone binding
    "zone" -> nonEmptyText,
    "meta" -> nonEmptyText
  ))

  val loginForm = Form(tuple(
    "email" -> nonEmptyText,
    "password" -> nonEmptyText
  ))

  val uniquenessForm = Form(
    // max length is a quick hack in order to now allow to fake BSONObjectID and
    // place it into the username
    "username" -> nonEmptyText(minLength = 3, maxLength = 20).verifying(
      "Username may contain only latin letters and numbers.",
      "^[a-zA-Z0-9]{3,}$".r.findFirstIn(_).isDefined
    )
  )

  val uniquenessFormError = uniquenessForm
                            .withError("username", "Not unique username. Choose another one.")

  val signupForm = Form(tuple(
    "email" -> email,
    "password" -> nonEmptyText
  ))

  val resetPasswordForm = Form(
    "email" -> email
  )

  val userLoginForm = Form(
    loginForm.mapping.verifying(
      "Wrong login or password",
      fields => fields match {
        case (eml, pwd) => MongoWait(userService.checkLoginAndPassword(eml, pwd))
      }
    )
  )

  val userSignUpForm = Form(
    signupForm.mapping.verifying(
      "This email has already been used.",
      fields => fields match {
        case (eml, _) => !MongoWait(userService.emailExists(eml))
      }
    )
  )
}
