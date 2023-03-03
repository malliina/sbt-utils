package com.malliina.codeartifact

import sbt.Keys.{credentials, publishMavenStyle, publishTo, resolvers, streams}
import sbt._
import software.amazon.awssdk.services.codeartifact.CodeartifactClient
import software.amazon.awssdk.services.codeartifact.model.GetAuthorizationTokenRequest
import concurrent.duration.DurationInt

object CodeArtifactKeys {
  val caDomain = settingKey[String]("CodeArtifact domain")
  val caDomainOwner = settingKey[String]("AWS account ID")
  val caRepo = settingKey[String]("CodeArtifact repository name")
  val caRepoHost = settingKey[String](
    "CodeArtifact repo host, e.g. xxx-111.d.codeartifact.eu-west-1.amazonaws.com"
  )
  val caRepoUrl = settingKey[String]("CodeArtifact repo URL")
}

object CodeArtifactPlugin extends AutoPlugin {
  // Adds settings automatically to the build; no need to use `.enablePlugins(...)`.
  override val trigger: PluginTrigger = allRequirements

  val autoImport = CodeArtifactKeys
  import CodeArtifactKeys._

  override def projectSettings: Seq[Def.Setting[?]] = Seq(
    resolvers += "CodeArtifact" at caRepoUrl.value,
    publishMavenStyle := true,
    publishTo := Some("CodeArtifact" at caRepoUrl.value),
    caRepoUrl := s"https://${caRepoHost.value}/maven/${caRepo.value}/",
    credentials += Credentials(
      s"${caDomain.value}/${caRepo.value}",
      caRepoHost.value,
      "aws",
      codeArtifactToken(caDomain.value, caDomainOwner.value, streams.value.log)
    )
  )

  // https://docs.aws.amazon.com/codeartifact/latest/ug/maven-mvn.html
  def codeArtifactToken(domain: String, domainOwner: String, log: Logger): String = {
    val fromEnv = sys.env.get("CODEARTIFACT_AUTH_TOKEN")
    def fromAws = {
      log.info(s"Fetching CodeArtifact token from AWS...")
      val request =
        GetAuthorizationTokenRequest
          .builder()
          .domain(domain)
          .domainOwner(domainOwner)
          .durationSeconds(12.hours.toSeconds)
          .build()
      CodeartifactClient.create().getAuthorizationToken(request).authorizationToken()
    }
    fromEnv getOrElse fromAws
  }
}
