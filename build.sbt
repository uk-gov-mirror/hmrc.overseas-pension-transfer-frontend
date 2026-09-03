import play.sbt.routes.RoutesKeys
import sbt.Def
import scoverage.ScoverageKeys
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

lazy val appName: String = "overseas-pension-transfer-frontend"

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "3.3.7"
Test / parallelExecution := false

addCommandAlias("runAllChecks", ";clean;compile;scalafmtAll;scalastyle;coverage;test;it/test;coverageReport")

val commonSettings: Seq[String] = Seq(
  "-unchecked",
  "-feature",
  "-deprecation",
  "-language:noAutoTupling",
  "-Wvalue-discard",
  "-Wunused:imports",
  "-Wconf:msg=unused import&src=views/.*:s", // False positives found in Twirl scala.html files at line 1:1: unused import
  "-Wconf:msg=unused private member&src=validators/.*:s", // False negative fixed in scala 3.7.0: see https://github.com/scala/scala3/issues/19998
  "-Wconf:msg=unused explicit parameter&src=pages/.*:s", // NextPageWith - it's not clear that the context argument is used - need a spike to investigate
  "-Werror",
  "-Wconf:src=routes/.*:s",
  "-Wunused:unsafe-warn-patvars",
  "-Wconf:msg=Flag.*repeatedly:s"
)

lazy val microservice = Project(appName, file("."))
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) // Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(inConfig(Test)(testSettings) *)
  .settings(ThisBuild / useSuperShell := false)
  .settings(
    name := appName,
    RoutesKeys.routesImport ++= Seq(
      "models._",
      "uk.gov.hmrc.play.bootstrap.binders.RedirectUrl"
    ),
    TwirlKeys.templateImports ++= Seq(
      "play.twirl.api.HtmlFormat",
      "play.twirl.api.HtmlFormat._",
      "uk.gov.hmrc.govukfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.helpers._",
      "uk.gov.hmrc.hmrcfrontend.views.config._",
      "views.ViewUtils._",
      "models.Mode",
      "controllers.routes._",
      "viewmodels.govuk.all._"
    ),
    PlayKeys.playDefaultPort := 15600,
    ScoverageKeys.coverageExcludedFiles := "<empty>;Reverse.*;.*handlers.*;.*components.*;" +
      ".*Routes.*;.*viewmodels.govuk.*;.*views.html.*;",
    ScoverageKeys.coverageMinimumStmtTotal := 78,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
    scalacOptions ++= commonSettings,
    libraryDependencies ++= AppDependencies(),
    retrieveManaged := true,
    pipelineStages := Seq(digest),
    Assets / pipelineStages := Seq(concat),
    scalafmtOnCompile := true
  )

lazy val testSettings: Seq[Def.Setting[?]] = Seq(
  fork := true,
  unmanagedSourceDirectories += baseDirectory.value / "test-utils"
)

lazy val it =
  (project in file("it"))
    .enablePlugins(PlayScala)
    .dependsOn(microservice % "test->test")
    .settings(scalacOptions ++= commonSettings)
