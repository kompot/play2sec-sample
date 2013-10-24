/*
 * Copyright (c) 2013
 */

package controllers

import play.api._
import play.api.mvc._

object Application extends Controller {
  
  def index = Action {
    Ok("Welcome to play2sec!")
  }
  
}
