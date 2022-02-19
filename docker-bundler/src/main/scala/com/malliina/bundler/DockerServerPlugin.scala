package com.malliina.bundler

import com.malliina.bundler.ClientPlugin.autoImport.assetsRoot
import com.malliina.bundler.ServerPlugin.autoImport.clientProject
import com.typesafe.sbt.SbtNativePackager.autoImport.NativePackagerHelper
import com.typesafe.sbt.packager.Keys.{daemonUser, defaultLinuxInstallLocation, packageName}
import com.typesafe.sbt.packager.archetypes.JavaServerAppPackaging
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport.{
  Docker,
  dockerBaseImage,
  dockerExposedPorts,
  dockerVersion
}
import com.typesafe.sbt.packager.docker.DockerVersion
import sbt.Keys.*
import sbt.{AutoPlugin, Compile, Def, Plugins, Project}

import scala.sys.process.Process
import scala.util.Try

object DockerServerPlugin extends AutoPlugin {
  val prodPort = 9000

  override def requires: Plugins = ServerPlugin && JavaServerAppPackaging

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    dockerVersion := Option(DockerVersion(19, 3, 5, None)),
    dockerBaseImage := "openjdk:11",
    dockerExposedPorts ++= Seq(prodPort),
    Docker / daemonUser := name.value,
    Docker / version := gitHash,
    Docker / packageName := name.value,
    Docker / mappings ++= Def.taskDyn {
      val dockerInstallDir = (Docker / defaultLinuxInstallLocation).value
      val p = Project.projectToRef(clientProject.value)
      Def.task {
        NativePackagerHelper
          .directory((p / Compile / assetsRoot).value.toFile)
          .map {
            case (file, path) =>
              val unixPath = path.replace('\\', '/')
              (file, s"$dockerInstallDir/$unixPath")
          }
      }
    }.value,
    Compile / doc / sources := Seq.empty
  )

  def gitHash: String =
    sys.env
      .get("GITHUB_SHA")
      .orElse(Try(Process("git rev-parse HEAD").lineStream.head).toOption)
      .getOrElse("unknown")
}
