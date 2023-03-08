lazy val log4j2V = "2.20.0"
lazy val bouncycastleV = "1.70"
lazy val scalatestV = "3.2.14"
lazy val scalacheckV = "3.2.11.0"

lazy val `learning-fibers` = project
  .in(file("."))
  .settings(
    name := """learning-fibers""",
    version := "0.1.0",
    scalaVersion := "3.2.2",
    organization := "info.galudisu",
    homepage := Some(url("https://github.com/barudisshu/learning-fibers")),
    licenses := List("MIT" -> url("https://opensource.org/licenses/MIT")),
    developers := List(
      Developer(
        "barudisshu",
        "Galudisu",
        "galudisu@gmail.com",
        url("https://galudisu.info")
      )
    ),
    Global / cancelable := false, // ctrl-c
    Compile / run / mainClass := Some("com.cplier.Main"),
    Compile / scalacOptions ++= Seq("-deprecation", "-Xfatal-warnings", "-feature", "-unchecked"),
    Compile / javacOptions ++= Seq("-Xlint:unchecked", "-Xlint:deprecation"),
    Compile / compileOrder := CompileOrder.JavaThenScala,
    run / javaOptions ++= Seq("-Xms128m", "-Xmx1024m", "-Djava.library.path=./target/native"),
    Test / fork / run := true,
    Test / compileOrder := CompileOrder.Mixed,
    resolvers ++= Seq("Local Maven Repository".at("file://" + Path.userHome.absolutePath + "/.m2/repository")) ++
      Resolver.sonatypeOssRepos("releases") ++
      Resolver.sonatypeOssRepos("snapshots"),
    Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-oDU"),
    Test / publishArtifact := false,
    // do not run test cases in parallel
    Test / parallelExecution := false,
    // show full stack traces and test case durations
    Test / logBuffered := false,
    libraryDependencies ++= Seq(
      // log4j2
      "org.apache.logging.log4j" % "log4j-api" % log4j2V,
      "org.apache.logging.log4j" % "log4j-core" % log4j2V,
      "org.apache.logging.log4j" % "log4j-slf4j-impl" % log4j2V,
      // bouncy castle JSSE provider
      "org.bouncycastle" % "bcpkix-jdk15on" % bouncycastleV,
      "org.bouncycastle" % "bctls-jdk15on" % bouncycastleV,
      // test
      "org.scalatest" %% "scalatest" % scalatestV % Test,
      "org.scalatestplus" %% "scalacheck-1-15" % scalacheckV % Test
    )
  )
