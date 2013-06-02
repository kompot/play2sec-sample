/*
 * Copyright (c) 2013
 */

package com.github.kompot.play2sec.authentication.providers

import com.github.kompot.play2sec.authentication.providers.password.DefaultUsernamePasswordAuthUser

class MyLoginUsernamePasswordAuthUser(clearPassword: String,
                                      email: String,
                                      expiration: Long)
    extends DefaultUsernamePasswordAuthUser(clearPassword, email) {

  def this(clearPassword: String, email: String) = this(clearPassword, email, System.currentTimeMillis() + 1000 * MyLoginUsernamePasswordAuthUser.SESSION_TIMEOUT)
//  def this(email: String) = this(null, email)

  override def expires = expiration
}

object MyLoginUsernamePasswordAuthUser {
  val SESSION_TIMEOUT: Long = 24 * 14 * 3600
}
