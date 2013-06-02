/*
 * Copyright (c) 2013
 */

package com.github.kompot.play2sec.authentication.providers.anonymous

import play.api.mvc.Request
import reactivemongo.bson.BSONObjectID
import com.github.kompot.play2sec.authentication.PlaySecPlugin
import com.github.kompot.play2sec.authentication.providers.AuthProvider
import com.github.kompot.play2sec.authentication.providers.password.Case
import com.github.kompot.play2sec.authentication

class AnonymousAuthProvider(implicit app: play.api.Application) extends AuthProvider(app) {
  def getKey = "anonymous"

  def authenticate[A](request: Request[A], payload: Option[Case.Value]): Any = {
    payload match {
      case Case.SIGNUP => {
        val user = new AnonymousAuthUser(BSONObjectID.generate.stringify)
        authentication.getUserService.save(user)
        user
      }
      case _ => {
        com.typesafe.plugin.use[PlaySecPlugin].login.url
      }
    }
  }

  protected def neededSettingKeys = List.empty

  def isExternal = false
}
