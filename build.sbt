import NativePackagerKeys._
Global / scalaJSStage := FullOptStage

lazy val server = (project in file("server"))
  .settings(commonSettings)
  .settings(
    scalaJSProjects := Seq(client),
    pipelineStages in Assets := Seq(scalaJSPipeline),
    pipelineStages := Seq(digest, gzip),
    // triggers scalaJSPipeline when using compile or continuous compilation
    compile in Compile := ((compile in Compile) dependsOn scalaJSPipeline).value,
    resolvers += "Sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
    resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
    resolvers += Resolver.sonatypeRepo("snapshots"),
    resolvers += Resolver.jcenterRepo,
    resolvers += Resolver.JCenterRepository,
    fork := true,
    libraryDependencies ++= Seq(
      "com.vmunier" %% "scalajs-scripts" % "1.1.4",
      // https://mvnrepository.com/artifact/org.reactivemongo/play2-reactivemongo
      "org.reactivemongo" %% "play2-reactivemongo" % "0.20.11-play28",
      "org.reactivemongo" %% "reactivemongo-play-json-compat" % "0.20.11-play28",
      "com.mohiva" %% "play-silhouette" % "6.1.0",
      "com.mohiva" %% "play-silhouette-password-bcrypt" % "6.1.0",
      "com.mohiva" %% "play-silhouette-persistence" % "6.1.0",
      "com.mohiva" %% "play-silhouette-crypto-jca" % "6.1.0",
      "com.mohiva" %% "play-silhouette-totp" % "6.1.0",
      "com.mohiva" %% "play-silhouette-testkit" % "6.1.0" % "test",
      "com.iheart" %% "ficus" % "1.4.7",
      // https://mvnrepository.com/artifact/com.enragedginger/akka-quartz-scheduler
      "com.enragedginger" %% "akka-quartz-scheduler" % "1.8.5-akka-2.6.x",
      // https://mvnrepository.com/artifact/net.codingwell/scala-guice
      "net.codingwell" %% "scala-guice" % "4.2.10",
      // https://mvnrepository.com/artifact/org.http4s/http4s-dsl
      "com.softwaremill.sttp.client" %% "core" % "2.2.3",
      "com.softwaremill.sttp.client" %% "async-http-client-backend-future" % "2.2.3",
      //test
      "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % "test",
      //json play
      "com.softwaremill.sttp.client" %% "play-json" % "2.2.0",
      // https://mvnrepository.com/artifact/com.dropbox.core/dropbox-core-sdk
      "com.dropbox.core" % "dropbox-core-sdk" % "3.1.4",
      "com.softwaremill.sttp.client" %% "core" % "2.2.3",
      "com.github.seratch" %% "awscala" % "0.8.+",
      // https://mvnrepository.com/artifact/net.dv8tion/JDA  //DISCORD REST
      "net.dv8tion" % "JDA" % "4.2.0_181",
      //refined,
      "be.venneborg" %% "play28-refined" % "0.6.0",
      //cats
      "org.typelevel" %% "cats-core" % "2.1.1",
      //test akka
      "org.scalactic" %% "scalactic" % "3.2.2",
      "org.scalatest" %% "scalatest" % "3.2.2" % "test",
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % "2.6.8" % Test,
      "com.typesafe.akka" %% "akka-actor-typed" % "2.6.8",
      "com.typesafe.akka" %% "akka-stream-typed" % "2.6.8",
      "org.scala-lang.modules" %% "scala-parallel-collections" % "1.0.0" % Test,
      "net.katsstuff" %% "ackcord" % "0.17.1", //For high level API, includes all the other modules
      "net.katsstuff" %% "ackcord-core" % "0.17.1", //Low level core API
      "net.katsstuff" %% "ackcord-commands" % "0.17.1", //Low to mid level Commands API
      "net.katsstuff" %% "ackcord-lavaplayer-core" % "0.17.1", //Low level lavaplayer API,
      ehcache,
      guice,
      filters,
      specs2 % Test
    ),
    Test / javaOptions ++= Seq(
      "--add-exports=java.base/sun.security.x509=ALL-UNNAMED"
      //"--add-opens=java.base/sun.security.ssl=ALL-UNNAMED" // Could be needed as well in some cases
    )
  )
  .enablePlugins(PlayScala)
  .dependsOn(shared.jvm)

lazy val client = (project in file("client"))
  .settings(commonSettings)
  .settings(
    scalaJSUseMainModuleInitializer := true,
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "1.0.0",
      "com.lihaoyi" %%% "utest" % "0.7.5" % "test"
    ),
    testFrameworks += new TestFramework("utest.runner.Framework"),
    scalacOptions ++= {
      import Ordering.Implicits._
      if (VersionNumber(scalaVersion.value).numbers >= Seq(2L, 13L)) {
        Seq("-Ymacro-annotations")
      } else {
        Nil
      }
    },
    jsEnv := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv(),
    libraryDependencies += "org.lrng.binding" %%% "html" % "1.0.3+6-55950506",
    libraryDependencies += "com.thoughtworks.binding" %%% "futurebinding" % "12.0.0"
  )
  .enablePlugins(ScalaJSPlugin, ScalaJSWeb)
  .dependsOn(shared.js)

lazy val shared = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("shared"))
  .settings(commonSettings)
  .settings(
    // https://mvnrepository.com/artifact/com.lihaoyi/upickle
    libraryDependencies += "com.lihaoyi" %%% "upickle" % "1.1.0",
    libraryDependencies += "org.scalatest" %%% "scalatest" % "3.2.2" % "test",
    libraryDependencies += "eu.timepit" %%% "refined" % "0.9.20"
  )
  .jsConfigure(_.enablePlugins(ScalaJSWeb))

lazy val commonSettings = Seq(
  scalaVersion := "2.13.1",
  organization := "gg.dsl"
)
