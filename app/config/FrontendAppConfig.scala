/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package config

import play.api.mvc.RequestHeader
import com.google.inject.Inject
import com.google.inject.Singleton
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import play.api.Configuration
import play.api.i18n.Lang

@Singleton
class FrontendAppConfig @Inject() (configuration: Configuration, servicesConfig: ServicesConfig) {

  import servicesConfig.*

  val appName: String = configuration.get[String]("appName")

  private val contactHost                  = configuration.get[String]("contact-frontend.host")
  private val contactFormServiceIdentifier = configuration.get[String]("contact-frontend.serviceId")

  def feedbackUrl(implicit request: RequestHeader): String =
    s"$contactHost/contact/beta-feedback?service=$contactFormServiceIdentifier&backUrl=${request.uri}"

  val loginUrl: String                = configuration.get[String]("urls.login")
  val loginContinueUrl: String        = configuration.get[String]("urls.loginContinue")
  val signOutUrl: String              = configuration.get[String]("urls.signOut")
  val pensionSchemeSummaryUrl: String = configuration.get[String]("urls.pensionSchemeUrl")
  val mpsHomeUrl: String              = configuration.get[String]("urls.mpsHomeUrl")

  private val exitSurveyBaseUrl: String = configuration.get[String]("feedback-frontend.host")
  val exitSurveyUrl: String             =
    s"$exitSurveyBaseUrl/feedback/report-transfer-qualifying-recognised-overseas-pension-scheme"

  def languageMap: Map[String, Lang] = Map(
    "en" -> Lang("en")
  )

  case class EnrolmentConfig(serviceName: String, identifierKey: String)

  private def loadEnrolmentConfig(role: String): EnrolmentConfig =
    EnrolmentConfig(
      configuration.get[String](s"enrolments.$role.serviceName"),
      configuration.get[String](s"enrolments.$role.identifierKey")
    )

  val psaEnrolment: EnrolmentConfig = loadEnrolmentConfig("psa")
  val pspEnrolment: EnrolmentConfig = loadEnrolmentConfig("psp")

  val timeout: Int   = configuration.get[Int]("timeout-dialog.timeout")
  val countdown: Int = configuration.get[Int]("timeout-dialog.countdown")

  val cacheTtl: Long = configuration.get[Int]("cache.ttlSeconds")

  val dashboardCacheTtl: Long = configuration.get[Long]("dashboard.ttlSeconds")

  val backendHost: String              = baseUrl("overseas-pension-transfer-backend")
  val backendService: String           = s"$backendHost/overseas-pension-transfer-backend"
  val pensionSchemeHost: String        = baseUrl("pensions-scheme")
  val pensionSchemeService: String     = s"$pensionSchemeHost/pensions-scheme"
  val pensionAdministratorHost: String = baseUrl("pension-administrator")
  val emailHost: String                = baseUrl("email")
  val emailService: String             = s"$emailHost/hmrc/email"

  val transfersPerPage: Int = configuration.get[Int]("pagination.transfersPerPage")

  val dashboardLockTtl: Long = configuration.get[Long]("dashboard.lockTtlSeconds")

  val signedOutRedirectUrl: String = configuration.get[String]("urls.signedOutRedirectUrl")

  val submittedConfirmationTemplateId: String = configuration.get[String]("submission-confirmation-template-id")

  def getPensionSchemeUrl(srn: String, isPspUser: Boolean): String =
    if (isPspUser) {
      s"${mpsHomeUrl.dropRight("/overview".length)}/$srn/dashboard/pension-scheme-details"
    } else {
      s"$pensionSchemeSummaryUrl$srn"
    }

  val mongoDBEncryption: Boolean = configuration.get[Boolean]("mongodb.encryption")
}
