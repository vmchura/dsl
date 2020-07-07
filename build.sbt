lazy val server = (project in file("server"))
  .settings(commonSettings)
  .settings(
    scalaJSProjects := Seq(client),
    pipelineStages in Assets := Seq(scalaJSPipeline),
    pipelineStages := Seq(digest, gzip),
    // triggers scalaJSPipeline when using compile or continuous compilation
    compile in Compile := ((compile in Compile) dependsOn scalaJSPipeline).value,
    resolvers += "Sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
    resolvers += Resolver.jcenterRepo,
    libraryDependencies ++= Seq(
      "com.vmunier" %% "scalajs-scripts" % "1.1.4",
      // https://mvnrepository.com/artifact/org.reactivemongo/play2-reactivemongo
      "org.reactivemongo" %% "play2-reactivemongo" % "0.20.11-play28",
      "org.reactivemongo" %% "reactivemongo-play-json-compat" %  "0.20.11-play28",
      "com.mohiva" %% "play-silhouette" % "6.1.0",
      "com.mohiva" %% "play-silhouette-password-bcrypt" % "6.1.0",
      "com.mohiva" %% "play-silhouette-persistence" % "6.1.0",
      "com.mohiva" %% "play-silhouette-crypto-jca" % "6.1.0",
      "com.mohiva" %% "play-silhouette-totp" % "6.1.0",
      "com.iheart" %% "ficus" % "1.4.7",
      // https://mvnrepository.com/artifact/com.enragedginger/akka-quartz-scheduler
      "com.enragedginger" %% "akka-quartz-scheduler" % "1.8.4-akka-2.6.x",
      // https://mvnrepository.com/artifact/net.codingwell/scala-guice
      "net.codingwell" %% "scala-guice" % "4.2.10",
      // https://mvnrepository.com/artifact/org.http4s/http4s-dsl
      "com.softwaremill.sttp.client" %% "core" % "2.2.0",
      "com.softwaremill.sttp.client" %% "async-http-client-backend-future" % "2.2.0",

      //test
      "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % "test",
      //json play
      "com.softwaremill.sttp.client" %% "play-json" % "2.2.0",

      ehcache,
        guice,
      specs2 % Test
    )
  )
  .enablePlugins(PlayScala)
  .dependsOn(shared.jvm)

lazy val client = (project in file("client"))
  .settings(commonSettings)
  .settings(
    scalaJSUseMainModuleInitializer := true,
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "1.0.0"
    )
  )
  .enablePlugins(ScalaJSPlugin, ScalaJSWeb)
  .dependsOn(shared.js)

lazy val shared = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("shared"))
  .settings(commonSettings)
  .jsConfigure(_.enablePlugins(ScalaJSWeb))


lazy val commonSettings = Seq(
  scalaVersion := "2.13.1",
  organization := "gg.dsl"
)
