// Comment to get more information during initialization
logLevel := Level.Warn

// Resolvers
resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

// Sbt plugins
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.7.4")
// Use Scala.js v1.x
addSbtPlugin("com.vmunier"               % "sbt-web-scalajs"           % "1.1.0")
addSbtPlugin("org.scala-js"              % "sbt-scalajs"               % "1.1.0")
// If you prefer using Scala.js v0.6.x, uncomment the following plugins instead:
// addSbtPlugin("com.vmunier"                  % "sbt-web-scalajs"           % "1.1.0-0.6")
// addSbtPlugin("org.scala-js"                 % "sbt-scalajs"               % "0.6.33")

addSbtPlugin("com.typesafe.play"         % "sbt-plugin"                % "2.8.2")
addSbtPlugin("org.portable-scala"        % "sbt-scalajs-crossproject"  % "1.0.0")
addSbtPlugin("com.typesafe.sbt"          % "sbt-gzip"                  % "1.0.2")
addSbtPlugin("com.typesafe.sbt"          % "sbt-digest"                % "1.1.4")
//To make the DOM available, add the following to your project/plugins.sbt:
libraryDependencies += "org.scala-js" %% "scalajs-env-jsdom-nodejs" % "1.0.0"