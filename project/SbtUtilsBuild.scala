import com.malliina.sbtutils.SbtProjects
import sbt.Keys._
import sbt._

object SbtUtilsBuild {
  lazy val sbtUtils = SbtProjects.sbtPlugin("sbt-utils").settings(projectSettings: _*)

  lazy val projectSettings = plugins ++ Seq(
    organization := "com.malliina"
  )

  def plugins = Seq(
    "com.jsuereth" % "sbt-pgp" % "1.0.0",
    "org.xerial.sbt" % "sbt-sonatype" % "1.1",
    "com.github.gseitz" % "sbt-release" % "1.0.3",
    "me.lessis" % "bintray-sbt" % "0.2.1"
  ) map addSbtPlugin
}
