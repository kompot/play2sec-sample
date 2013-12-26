import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "play2sec-sample"
  val appVersion      = "0.0.1-SNAPSHOT"

  val appDependencies = Seq(
    "com.softwaremill.macwire" %% "core"                % "0.4.1",
    "com.github.kompot"        %% "play2sec"            % "0.0.1",
//    "org.apache.commons" % "commons-email"            % "1.2",
    "org.reactivemongo"        %% "reactivemongo"       % "0.10.0",
    "org.reactivemongo"        %% "play2-reactivemongo" % "0.10.0",
    "com.typesafe"             %% "play-plugins-mailer" % "2.2.0"
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
  )

}
