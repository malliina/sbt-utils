scalaVersion := "2.12.4"

resolvers ++= Seq(
  ivyResolver("bintray-sbt-plugin-releases", "http://dl.bintray.com/content/sbt/sbt-plugin-releases"),
  ivyResolver("malliina bintray sbt", "https://dl.bintray.com/malliina/sbt-plugins/")
)

def ivyResolver(name: String, urlStr: String) =
  Resolver.url(name, url(urlStr))(Resolver.ivyStylePatterns)

Seq(
  "org.xerial.sbt" % "sbt-sonatype" % "2.0",
  "com.github.gseitz" % "sbt-release" % "1.0.6",
  "com.jsuereth" % "sbt-pgp" % "1.1.0",
  "org.foundweekends" % "sbt-bintray" % "0.5.1"
) map addSbtPlugin
