/*
 * Copyright (c) 2013
 */

package com.github.kompot.play2sec.authentication.providers.anonymous

import com.github.kompot.play2sec.authentication.user.AuthUser

class AnonymousAuthUser(id: String) extends AuthUser {
  def getId = id

  def getProvider = "anonymous"
}
