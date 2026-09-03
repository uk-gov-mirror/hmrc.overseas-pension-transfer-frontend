import sbt.*

object AppDependencies {

  private val bootstrapVersion = "10.7.0"
  private val hmrcMongoVersion = "2.13.0"
  private val PekkoVersion     = "1.4.0"

  val compile: Seq[ModuleID] = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"                  %% "play-frontend-hmrc-play-30" % "13.11.0",
    "uk.gov.hmrc"                  %% "bootstrap-frontend-play-30" % bootstrapVersion,
    "uk.gov.hmrc.mongo"            %% "hmrc-mongo-play-30"         % hmrcMongoVersion,
    "org.typelevel"                %% "cats-core"                  % "2.13.0",
    "com.googlecode.libphonenumber" % "libphonenumber"             % "9.0.35",
    "io.github.samueleresca"       %% "pekko-quartz-scheduler"     % "1.3.0-pekko-1.1.x" withSources (),

    // Explicit pekko dependencies to ensure version alignment
    "org.apache.pekko" %% "pekko-actor"                 % PekkoVersion,
    "org.apache.pekko" %% "pekko-actor-typed"           % PekkoVersion,
    "org.apache.pekko" %% "pekko-stream"                % PekkoVersion,
    "org.apache.pekko" %% "pekko-serialization-jackson" % PekkoVersion,
    "org.apache.pekko" %% "pekko-slf4j"                 % PekkoVersion,
    "org.apache.pekko" %% "pekko-protobuf-v3"           % PekkoVersion
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"          %% "bootstrap-test-play-30"  % bootstrapVersion,
    "uk.gov.hmrc.mongo"    %% "hmrc-mongo-test-play-30" % hmrcMongoVersion,
    "org.scalatestplus"    %% "scalacheck-1-17"         % "3.2.18.0",
    "io.github.wolfendale" %% "scalacheck-gen-regexp"   % "1.1.0",
    "org.jsoup"             % "jsoup"                   % "1.21.2"
  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test
}
