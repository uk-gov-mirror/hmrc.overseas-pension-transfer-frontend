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

package connectors

import com.google.inject.Inject
import config.FrontendAppConfig
import connectors.parsers.PensionSchemeParser.*
import models.PensionSchemeResponse
import models.authentication.{AuthenticatedUser, PsaId, PsaUser, PspUser}
import models.responses.{PensionSchemeErrorResponse, PensionSchemeNotAssociated}
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.libs.json.{JsError, JsSuccess, Reads, __}
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps, UpstreamErrorResponse}
import utils.DownstreamLogging

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class PensionSchemeConnector @Inject() (
  appConfig: FrontendAppConfig,
  http: HttpClientV2,
  httpClientResponse: HttpClientResponse
)(implicit ec: ExecutionContext)
    extends DownstreamLogging {

  private val authorisingPsaIdFromApiReads: Reads[PsaId] =
    (__ \ "pspDetails" \ "authorisingPSAID").read[String].map(PsaId.apply)

  def checkAssociation(srn: String, user: AuthenticatedUser)(implicit hc: HeaderCarrier): Future[Boolean] = {
    val url        = url"${appConfig.pensionSchemeService}/is-psa-associated"
    val userHeader =
      user match {
        case PsaUser(psaId, _, _) => "psaId" -> psaId.value
        case PspUser(pspId, _, _) => "pspId" -> pspId.value
      }
    httpClientResponse
      .read(
        http
          .get(url)
          .setHeader(
            "schemeReferenceNumber" -> srn,
            userHeader
          )
          .execute[Either[UpstreamErrorResponse, HttpResponse]]
      )
      .value
      .collect {
        case Left(x: UpstreamErrorResponse) =>
          logger.warn(s"[PensionSchemeConnector][checkAssociation] ${x.message}")
          false
        case Right(x: HttpResponse)         => Try(x.json.as[Boolean]).getOrElse(false)
      }
  }

  def getAuthorisingPsa(srn: String)(implicit hc: HeaderCarrier): Future[AuthorisingPsaIdType] = {
    val url = url"${appConfig.pensionSchemeService}/psp-scheme/$srn"

    httpClientResponse
      .read(
        http
          .get(url)
          .setHeader(
            "srn" -> srn
          )
          .execute[Either[UpstreamErrorResponse, HttpResponse]]
      )
      .value
      .collect {
        case Left(x: UpstreamErrorResponse) if x.statusCode == NOT_FOUND => Left(new PensionSchemeNotAssociated)
        case Left(x: UpstreamErrorResponse)                              =>
          logger.warn(s"[PensionSchemeConnector][getAuthorisingPsa] ${x.message}")
          Left(PensionSchemeErrorResponse(x.message, None))
        case Right(x: HttpResponse) if x.status == OK                    =>
          x.json.validate[PsaId](authorisingPsaIdFromApiReads) match {
            case JsSuccess(value, _) =>
              Right(value)
            case JsError(errors)     =>
              val formatted = formatJsonErrors(errors)
              logger.warn(
                s"[PensionSchemeConnector][getAuthorisingPsaId] Unable to parse JSON as AuthorisingPsaId: $formatted"
              )
              Left(PensionSchemeErrorResponse("Unable to parse JSON as AuthorisingPsaId", Some(formatted)))
          }
      }
  }

  def getSchemeDetails(srn: String, authenticatedUser: AuthenticatedUser)(implicit
    hc: HeaderCarrier
  ): Future[PensionSchemeDetailsType] = {
    val (url, headers) = authenticatedUser match {
      case PsaUser(_, _, _) =>
        (url"${appConfig.pensionSchemeService}/scheme/$srn", Seq("schemeIdType" -> "srn", "idNumber" -> srn))
      case PspUser(_, _, _) => (url"${appConfig.pensionSchemeService}/psp-scheme/$srn", Seq("srn" -> srn))
    }

    httpClientResponse
      .read(
        http
          .get(url)
          .setHeader(
            headers: _*
          )
          .execute[Either[UpstreamErrorResponse, HttpResponse]]
      )
      .value
      .collect {
        case Left(x: UpstreamErrorResponse) if x.statusCode == NOT_FOUND => Left(new PensionSchemeNotAssociated)
        case Right(x: HttpResponse) if x.status == OK                    =>
          x.json.validate[PensionSchemeResponse] match {
            case JsSuccess(value, _) => Right(value)
            case JsError(errors)     =>
              val formatted = formatJsonErrors(errors)
              logger.warn(
                s"[PensionSchemeConnector][getSchemeDetails] Unable to parse JSON as PensionSchemeData: $formatted"
              )
              Left(PensionSchemeErrorResponse("Unable to parse JSON as PensionSchemeData", Some(formatted)))
          }
        case Left(x: UpstreamErrorResponse)                              =>
          logger.warn(s"[PensionSchemeConnector][getSchemeDetails] ${x.message}")
          Left(PensionSchemeErrorResponse(x.message, None))
      }
  }
}
