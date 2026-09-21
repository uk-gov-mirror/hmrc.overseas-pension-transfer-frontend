/*
 * Copyright 2025 HM Revenue & Customs
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

import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

object TestAppConfig {
  private val configItems: Seq[(String, Any)] = Seq(
    "appName"                             -> "test-frontend",
    "contact-frontend.host"               -> "http://localhost:9250",
    "contact-frontend.serviceId"          -> "test-service",
    "urls.login"                          -> "http://localhost:9553/login",
    "urls.loginContinue"                  -> "http://localhost:9000/continue",
    "urls.signOut"                        -> "http://localhost:9000/sign-out",
    "urls.signedOutRedirectUrl"           -> "http://localhost:15600/report-transfer-qualifying-recognised-overseas-pension-scheme/account/signed-out",
    "urls.pensionSchemeUrl"               -> "http://localhost:8204/manage-pension-schemes/pension-scheme-summary/",
    "urls.mpsHomeUrl"                     -> "http://localhost:8204/manage-pension-schemes/overview",
    "feedback-frontend.host"              -> "http://localhost:9514",
    "enrolments.psa.serviceName"          -> "HMRC-PSA-ORG",
    "enrolments.psa.identifierKey"        -> "PSAID",
    "enrolments.psp.serviceName"          -> "HMRC-PSP-ORG",
    "enrolments.psp.identifierKey"        -> "PSPID",
    "timeout-dialog.timeout"              -> 900,
    "timeout-dialog.countdown"            -> 120,
    "cache.ttlSeconds"                    -> 300,
    "dashboard.ttlSeconds"                -> 600,
    "pagination.transfersPerPage"         -> 10,
    "dashboard.lockTtlSeconds"            -> 30,
    "pension-scheme-summary.service-path" -> "/manage-pension-schemes/pension-scheme-summary/",
    "submission-confirmation-template-id" -> "overseas_transfer_charge_confirm_transfer_submitted"
  )

  private val testConfigurationEncryptionOn = {
    val items: Seq[(String, Any)] = configItems ++ Seq("mongodb.encryption" -> true)
    Configuration(items*)
  }

  private val servicesConfig = new ServicesConfig(
    Configuration(
      "microservice.services.overseas-pension-transfer-backend.host" -> "http://localhost",
      "microservice.services.overseas-pension-transfer-backend.port" -> 9999,
      "microservice.services.pensions-scheme.host"                   -> "http://localhost",
      "microservice.services.pensions-scheme.port"                   -> 8203,
      "microservice.services.email.host"                             -> "http://localhost",
      "microservice.services.email.port"                             -> 8300,
      "microservice.services.pension-administrator.host"             -> "http://localhost",
      "microservice.services.pension-administrator.port"             -> 8205
    )
  )

  def appConfigEncryptionOn(): FrontendAppConfig = new FrontendAppConfig(testConfigurationEncryptionOn, servicesConfig)
}
