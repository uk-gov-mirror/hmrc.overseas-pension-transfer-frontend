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

package connectors

import config.FrontendAppConfig
import models.MinimalDetails
import models.authentication.{PsaId, PspId}
import play.api.http.Status.NOT_FOUND
import play.api.libs.json.{JsError, JsSuccess}
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps, UpstreamErrorResponse}
import utils.DownstreamLogging

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

sealed trait MinimalDetailsError

case object UpstreamError extends MinimalDetailsError
case object DetailsNotFound extends MinimalDetailsError

class MinimalDetailsConnector @Inject() (
  appConfig: FrontendAppConfig,
  http: HttpClientV2,
  httpClientResponse: HttpClientResponse
) extends DownstreamLogging {

  private val url = url"${appConfig.pensionAdministratorHost}/pension-administrator/get-minimal-details-self"

  def fetch(
    psaId: PsaId
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[MinimalDetailsError, MinimalDetails]] =
    fetch("psaId", psaId.value, loggedInAsPsa = true)

  def fetch(
    pspId: PspId
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[MinimalDetailsError, MinimalDetails]] =
    fetch("pspId", pspId.value, loggedInAsPsa = false)

  private def fetch(
    idType: String,
    idValue: String,
    loggedInAsPsa: Boolean
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[MinimalDetailsError, MinimalDetails]] =
    httpClientResponse
      .read(
        http
          .get(url)
          .setHeader(idType -> idValue, "loggedInAsPsa" -> loggedInAsPsa.toString)
          .execute[Either[UpstreamErrorResponse, HttpResponse]]
      )
      .value
      .map {
        case Left(x: UpstreamErrorResponse) =>
          x.statusCode match {
            case NOT_FOUND if x.message.contains("no match found") => Left(DetailsNotFound)
            case _                                                 =>
              logger.error(s"[MinimalDetailsConnector][fetch] Upstream error occurred ${x.message}", x)
              Left(UpstreamError)
          }
        case Right(x: HttpResponse)         =>
          x.json.validate[MinimalDetails] match {
            case JsError(err)        =>
              logger.warn(
                s"[MinimalDetailsConnector][fetch] Unable to parse Json as GetAllTransfersDTO: ${formatJsonErrors(err)}"
              )
              Left(UpstreamError)
            case JsSuccess(value, _) => Right(value)
          }
      }
}
