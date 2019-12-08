scalaVersion := "2.12.10"

resolvers ++= Seq(
  ivyResolver("bintray-sbt-plugin-releases", "https://dl.bintray.com/content/sbt/sbt-plugin-releases"),
  ivyResolver("malliina bintray sbt", "https://dl.bintray.com/malliina/sbt-plugins/")
)

def ivyResolver(name: String, urlStr: String) =
  Resolver.url(name, url(urlStr))(Resolver.ivyStylePatterns)

Seq(
  "org.xerial.sbt" % "sbt-sonatype" % "3.7",
  "com.github.gseitz" % "sbt-release" % "1.0.11",
  "com.jsuereth" % "sbt-pgp" % "1.1.2",
//  "org.foundweekends" % "sbt-bintray" % "0.5.4",
  "org.scalameta" % "sbt-mdoc" % "1.3.2"
) map addSbtPlugin
