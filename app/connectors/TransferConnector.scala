/*
 * Copyright 2023 HM Revenue & Customs
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
import models.dtos.GetAllTransfersDTO
import models.responses.{AllTransfersUnexpectedError, InternalServerError, NoTransfersFound, TransferError}
import models.{PstrNumber, SrnNumber}
import play.api.Logging
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND, OK}
import play.api.libs.json.{JsError, JsSuccess}
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps, UpstreamErrorResponse}
import utils.DownstreamLogging

import java.net.URL
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TransferConnector @Inject() (
  appConfig: FrontendAppConfig,
  http: HttpClientV2,
  httpResponse: HttpClientResponse
) extends Logging
    with DownstreamLogging {

  type GetAllTransfersType = Either[TransferError, GetAllTransfersDTO]

  def getAllTransfers(srnNumber: SrnNumber, pstrNumber: PstrNumber)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[GetAllTransfersType] = {
    def allTransfersUrl: URL =
      url"${appConfig.backendService}/get-all-transfers/${pstrNumber.value}"

    httpResponse
      .read(
        http
          .get(allTransfersUrl)
          .setHeader("schemeReferenceNumber" -> srnNumber.value)
          .execute[Either[UpstreamErrorResponse, HttpResponse]]
      )
      .value
      .map {
        case Left(x: UpstreamErrorResponse) =>
          x.statusCode match {
            case NOT_FOUND             =>
              Left(NoTransfersFound)
            case INTERNAL_SERVER_ERROR =>
              Left(InternalServerError)
            case statusCode            =>
              Left(AllTransfersUnexpectedError(s"Unexpected status code returned from backend: $statusCode", None))
          }
        case Right(x)                       =>
          validateAndPartitionDTO(x)
      }
  }

  private def validateAndPartitionDTO(x: HttpResponse) =
    x.status match {
      case OK         =>
        x.json.validate[GetAllTransfersDTO] match {
          case JsError(errors)   =>
            val formatted = formatJsonErrors(errors)
            logger.warn(
              s"[TransferConnector][getAllTransfers] Unable to parse Json as GetAllTransfersDTO: $formatted"
            )
            Left(AllTransfersUnexpectedError("Unable to parse Json as GetAllTransfersDTO", Some(formatted)))
          case JsSuccess(dto, _) =>
            val (valid, notValid) = dto.transfers.partition(_.isValid)
            if (notValid.nonEmpty) {
              logger.warn(
                s"[TransferConnector][getAllTransfers] Dropping ${notValid.size} " +
                  s"invalid transfer items (must have exactly one of submissionDate or lastUpdated)."
              )
            }
            Right(dto.copy(transfers = valid))
        }
      case statusCode =>
        logger.warn(s"[TransferConnector][getAllTransfers] Unexpected status code return: $statusCode")
        Left(AllTransfersUnexpectedError(s"Unexpected status code returned from backend: $statusCode", None))
    }
}
