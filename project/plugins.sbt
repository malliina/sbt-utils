scalaVersion := "2.10.6"

resolvers ++= Seq(
  ivyResolver("bintray-sbt-plugin-releases", "http://dl.bintray.com/content/sbt/sbt-plugin-releases"),
  ivyResolver("malliina bintray sbt", "https://dl.bintray.com/malliina/sbt-plugins/")
)

def ivyResolver(name: String, urlStr: String) =
  Resolver.url(name, url(urlStr))(Resolver.ivyStylePatterns)

Seq(
  // self-reference!
  "com.malliina" %% "sbt-utils" % "0.6.0",
  "com.jsuereth" % "sbt-pgp" % "1.0.0",
  "org.xerial.sbt" % "sbt-sonatype" % "1.1",
  "com.github.gseitz" % "sbt-release" % "1.0.3",
  "me.lessis" % "bintray-sbt" % "0.2.1"
) map addSbtPlugin
