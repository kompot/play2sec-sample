import play.Project._

name := "play2sec-sample"

organization := "com.github.kompot"

version := "0.0.1-SNAPSHOT"

libraryDependencies ++= Seq(
  "com.softwaremill.macwire" %% "core"                % "0.4.1",
  "com.github.kompot"        %% "play2sec"            % "0.0.1",
  //    "org.apache.commons" % "commons-email"            % "1.2",
  "org.reactivemongo"        %% "reactivemongo"       % "0.10.0",
  "org.reactivemongo"        %% "play2-reactivemongo" % "0.10.0",
  "com.typesafe"             %% "play-plugins-mailer" % "2.2.0"
)

play.Project.playScalaSettings ++ Seq(
  resolvers += "Sonatype Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/",
  resolvers += "Common maven repository" at "http://repo1.maven.org/maven2/",
  resolvers += "Local maven repository" at "file://" + Path.userHome.absolutePath + "/.m2/repository/"
)
