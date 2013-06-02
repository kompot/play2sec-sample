/*
 * Copyright (c) 2013
 */

package com.github.kompot.play2sec.authorization.handler

import play.api.mvc.{Request, Result, Results}
import com.github.kompot.play2sec.authorization.scala._
import com.github.kompot.play2sec.authorization.core.models.Subject
import com.github.kompot.play2sec.authentication

class CustomDeadboltHandler(dynamicResourceHandler: Option[DynamicResourceHandler] = None) extends DeadboltHandler {
  val userService = bootstrap.Global.Injector.userService

  def beforeAuthCheck[A](request: Request[A]) = None

  override def getDynamicResourceHandler[A](request: Request[A]): Option[DynamicResourceHandler] = {
    if (dynamicResourceHandler.isDefined)
      dynamicResourceHandler
    else
      Some(new MyDynamicResourceHandler())
  }

  override def getSubject[A](request: Request[A]): Option[Subject] = {
    authentication.getUser(request).map(userService.getByAuthUserIdentitySync(_)).getOrElse(None)
  }

  def onAuthFailure[A](request: Request[A]): Result = {
    Results.Forbidden("Access denied.")
  }
}
