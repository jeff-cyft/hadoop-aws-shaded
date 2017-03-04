import ohnosequences.sbt.SbtS3Resolver
import com.amazonaws.auth.InstanceProfileCredentialsProvider
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.services.s3.model.Region

organization := "io.cyft"
name := "hadoop-aws"
version := "2.7.3"

// Do not append Scala versions to the generated artifacts
crossPaths := false

// This forbids including Scala related libraries into the dependency
autoScalaLibrary := false



lazy val root = (project in file("."))
  .enablePlugins(SbtS3Resolver)
resolvers += Resolver.mavenLocal
resolvers ++= Seq[Resolver](
  s3resolver.value("Releases resolver", s3("releases.mvn-repo.cyft.io")).withIvyPatterns,
  s3resolver.value("Snapshots resolver", s3("snapshots.mvn-repo.cyft.io")).withIvyPatterns
)

libraryDependencies ++= Seq(
  "org.apache.hadoop" % "hadoop-aws" % "2.7.3"
    exclude("org.apache.hadoop","hadoop-common")
    exclude("commons-logging", "commons-logging")
    exclude("commons-codec", "commons-codec")
    exclude("joda-time", "joda-time")
)

assemblyShadeRules in assembly := Seq(
  ShadeRule.rename("com.amazonaws.**" -> "shaded.@1").inAll,
  ShadeRule.rename("javax.servlet.**" -> "shaded.@1").inAll,
  ShadeRule.rename("com.fasterxml.**" -> "shaded.@1").inAll,
  ShadeRule.rename("org.apache.http.**" -> "shaded.@1").inAll
)

artifact in (Compile, assembly) := {
  val art = (artifact in (Compile, assembly)).value
  art.copy(`classifier` = Some("shaded"))
}

addArtifact(artifact in (Compile, assembly), assembly)

// Releasing
// =========
awsProfile := "default"

s3credentials :=
  new ProfileCredentialsProvider(awsProfile.value) | new InstanceProfileCredentialsProvider()

s3region := Region.US_Standard

publishMavenStyle := false
publishTo := {
  val prefix = if (isSnapshot.value) "snapshots" else "releases"

  Some(s3resolver.value(s"$prefix s3 bucket", s3(prefix+".mvn-repo.cyft.io")) withIvyPatterns)
}
