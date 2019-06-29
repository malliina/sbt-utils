scalaVersion := "2.12.8"

resolvers ++= Seq(
  ivyResolver("bintray-sbt-plugin-releases", "http://dl.bintray.com/content/sbt/sbt-plugin-releases"),
  ivyResolver("malliina bintray sbt", "https://dl.bintray.com/malliina/sbt-plugins/")
)

def ivyResolver(name: String, urlStr: String) =
  Resolver.url(name, url(urlStr))(Resolver.ivyStylePatterns)

Seq(
  "org.xerial.sbt" % "sbt-sonatype" % "2.5",
  "com.github.gseitz" % "sbt-release" % "1.0.11",
  "com.jsuereth" % "sbt-pgp" % "1.1.2",
  "org.foundweekends" % "sbt-bintray" % "0.5.4"
) map addSbtPlugin
