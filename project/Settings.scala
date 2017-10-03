import sbt._
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._

/**
 * Application settings. Configure the build for your application here.
 * You normally don't have to touch the actual build definition after this.
 */
object Settings {
  /** The name of your application */
  val name = "evilplot"
  val organization = "com.cibo"

  /** The version of your application */
  lazy val buildVersion = sys.env.getOrElse("TRAVIS_BUILD_NUMBER", (System.currentTimeMillis / 1000).toString)
  val version = s"0.1.$buildVersion"

  /** Options for the scala compiler */
  val scalacOptions = Seq(
    "-deprecation",
    // "-Xlint",
    "-unchecked"
    // "-deprecation",
    // "-feature",
    // "-J-Xss1024m"
  )

  /** Declare global dependency versions here to avoid mismatches in multi part dependencies */
  object versions { //scalastyle:ignore
    val scala = "2.12.3"
    val scalaDom = "0.9.3"
    val scalaTest = "3.0.1"
    val scalactic = "3.0.1"
    val scopt = "3.5.0"
    val circe = "0.7.0"
  }

  /**
   * These dependencies are shared between JS and JVM projects
   * the special %%% function selects the correct version for each project
   */
  val sharedDependencies = Def.setting(Seq(
    "io.circe" %%% "circe-core" % versions.circe,
    "io.circe" %%% "circe-generic" % versions.circe,
    "io.circe" %%% "circe-parser" % versions.circe,
    "io.circe" %%% "circe-generic-extras" % versions.circe,
    "org.scalactic" %%% "scalactic" % versions.scalactic,
    "org.scalatest" %%% "scalatest" % versions.scalaTest % "test",
    compilerPlugin("org.scalamacros" %% "paradise" % "2.1.0" cross CrossVersion.full)
  ))

  /** Dependencies only used by the JVM project */
  val jvmDependencies = Def.setting(Seq(
    // "com.github.scopt" %% "scopt" % "3.5.0"
  ))

  /** Dependencies only used by the JS project (note the use of %%% instead of %%) */
  val scalajsDependencies = Def.setting(Seq(
    "org.scala-js" %%% "scalajs-dom" % versions.scalaDom
  ))

  /** Dependencies for external JS libs that are bundled into a single .js file according to dependency order */
  val jsDependencies = Def.setting(Seq(
  ))
}
