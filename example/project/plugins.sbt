val rollup = ProjectRef(file("../.."), "sbt-revolver-rollup")
val node = ProjectRef(file("../.."), "sbt-nodejs")
val root = project.in(file(".")).dependsOn(rollup, node)
