trait SbtGit {
  def gitPom(projectName: String, gitUser: String, realName: String, developerHomePage: String) =
    (<url>https://github.com/{gitUser}/{projectName}</url>
      <scm>
        <url>git@github.com:{gitUser}/{projectName}.git</url>
        <connection>scm:git:git@github.com:{gitUser}/{projectName}.git</connection>
      </scm>
      <developers>
        <developer>
          <id>{gitUser}</id>
          <name>{realName}</name>
          <url>{developerHomePage}</url>
        </developer>
      </developers>)
}
object SbtGit extends SbtGit
