scalaVersion := "2.12.10"

resolvers ++= Seq(
  ivyResolver(
    "bintray-sbt-plugin-releases",
    "https://dl.bintray.com/content/sbt/sbt-plugin-releases"
  ),
  ivyResolver("malliina bintray sbt", "https://dl.bintray.com/malliina/sbt-plugins/")
)

def ivyResolver(name: String, urlStr: String) =
  Resolver.url(name, url(urlStr))(Resolver.ivyStylePatterns)

Seq(
  "org.xerial.sbt" % "sbt-sonatype" % "3.8.1",
  "com.github.gseitz" % "sbt-release" % "1.0.13",
  "com.jsuereth" % "sbt-pgp" % "2.0.1",
  "org.scalameta" % "sbt-mdoc" % "1.3.2"
) map addSbtPlugin
