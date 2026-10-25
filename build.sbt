ThisBuild / scalaVersion := "3.9.0"

lazy val root = (project in file("."))
  .settings(
    name := "Lune Core",
    organization := "net.lunapixu",
    version := "0.2.0",
    artifactName := { (sv: ScalaVersion, module: ModuleID, artifact: Artifact) =>
      artifact.name + '-' + module.revision + '.' + artifact.extension
    }
  )

libraryDependencies += "io.papermc.paper" % "paper-api" % "1.21.4-R0.1-SNAPSHOT" % Provided
libraryDependencies += "net.luckperms" % "api" % "5.4" % Provided

resolvers += Resolver.sonatypeCentralSnapshots
resolvers += "papermc" at "https://repo.papermc.io/repository/maven-public"