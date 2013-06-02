import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "play2sec-sample"
  val appVersion      = "0.0.1"

  val appDependencies = Seq(
    "com.softwaremill.macwire" %% "core" % "0.2",
    "com.github.kompot" %% "play2sec" % "0.0.1",
    "org.apache.commons" % "commons-email" % "1.2",
    "org.reactivemongo" %% "reactivemongo" % "0.9",
    "org.reactivemongo" %% "play2-reactivemongo" % "0.9"
  )

  def customLessEntryPoints(base: File): PathFinder = (
    (base / "app" / "assets" / "stylesheets" / "tbs" * "bootstrap.less")
  )


  val main = play.Project(appName, appVersion, appDependencies).settings(
    lessEntryPoints <<= baseDirectory(customLessEntryPoints)
  )

}
