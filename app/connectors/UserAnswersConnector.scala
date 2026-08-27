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
import connectors.parsers.UserAnswersParser.*
import models.*
import models.dtos.{SubmissionDTO, UserAnswersDTO}
import models.responses.*
import org.apache.pekko.Done
import play.api.Logging
import play.api.http.Status.{BAD_REQUEST, NOT_FOUND, NO_CONTENT, OK}
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps, UpstreamErrorResponse}
import utils.DownstreamLogging

import java.net.URL
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class UserAnswersConnector @Inject() (
  appConfig: FrontendAppConfig,
  http: HttpClientV2,
  httpClientResponse: HttpClientResponse
) extends Logging
    with DownstreamLogging {

  // These two versions of getAnswers are purposely similar to one another as it is recommended to combine these two in a future refactor
  def getAnswers(transferId: String, srnNumber: SrnNumber)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[GetUserAnswersType] =
    httpClientResponse
      .read(
        http
          .get(url"${appConfig.backendService}/save-for-later/$transferId")
          .setHeader("schemeReferenceNumber" -> srnNumber.value)
          .execute[Either[UpstreamErrorResponse, HttpResponse]]
      )
      .value
      .map {
        case Left(x: UpstreamErrorResponse) if x.statusCode == NOT_FOUND =>
          logger.warn("[UserAnswersConnector][getAnswers] No record was found in save for later}")
          Left(UserAnswersNotFoundResponse)
        case Left(x: UpstreamErrorResponse)                              =>
          Left(UserAnswersErrorResponse(x.message, None))
        case Right(x: HttpResponse)                                      =>
          x.status match {
            case OK if Try(x.json).toOption.isEmpty =>
              logger.warn("[UserAnswersConnector][getAnswers] Empty response body")
              Left(UserAnswersErrorResponse("Empty response body", None))
            case OK                                 =>
              x.json.validate[UserAnswersDTO] match {
                case JsSuccess(value, _) => Right(value)
                case JsError(errors)     =>
                  val formatted = formatJsonErrors(errors)
                  logger.warn(s"[UserAnswersConnector][getAnswers] Unable to parse Json as UserAnswersDTO: $formatted")
                  Left(UserAnswersErrorResponse("Unable to parse Json as UserAnswersDTO", Some(formatted)))
              }
          }
      }

  def getAnswers(
    transferId: TransferId,
    pstrNumber: PstrNumber,
    qtStatus: QtStatus,
    versionNumber: Option[String] = None,
    srnNumber: SrnNumber
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[GetUserAnswersType] = {

    def url: URL =
      url"${appConfig.backendService}/get-transfer/${transferId.value}"

    val queryStringParams =
      Seq("pstr" -> pstrNumber.value, "qtStatus" -> qtStatus.toString) ++ versionNumber.toSeq.map("versionNumber" -> _)

    httpClientResponse
      .read(
        http
          .get(url)
          .transform(_.addQueryStringParameters(queryStringParams: _*))
          .setHeader("schemeReferenceNumber" -> srnNumber.value)
          .execute[Either[UpstreamErrorResponse, HttpResponse]]
      )
      .value
      .map {
        case Left(x: UpstreamErrorResponse) =>
          x.statusCode match {
            case NOT_FOUND =>
              logger.warn("[UserAnswersConnector][getAnswers] No record was found in save for later}")
              Left(UserAnswersNotFoundResponse)
            case _         =>
              Left(UserAnswersErrorResponse(x.message, None))
          }
        case Right(x: HttpResponse)         =>
          x.status match {
            case OK if Try(x.json).toOption.isEmpty =>
              logger.warn("[UserAnswersConnector][getAnswers] Empty response body")
              Left(UserAnswersErrorResponse("Empty response body", None))
            case OK                                 =>
              x.json.validate[UserAnswersDTO] match {
                case JsSuccess(value, _) => Right(value)
                case JsError(errors)     =>
                  val formatted = formatJsonErrors(errors)
                  logger.warn(s"[UserAnswersConnector][getAnswers] Unable to parse Json as UserAnswersDTO: $formatted")
                  Left(UserAnswersErrorResponse("Unable to parse Json as UserAnswersDTO", Some(formatted)))
              }
          }
      }
  }

  def putAnswers(
    userAnswersDTO: UserAnswersDTO,
    srnNumber: SrnNumber
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[SetUserAnswersType] =
    httpClientResponse
      .read(
        http
          .post(url"${appConfig.backendService}/save-for-later")
          .setHeader("schemeReferenceNumber" -> srnNumber.value)
          .withBody(Json.toJson(userAnswersDTO))
          .execute[Either[UpstreamErrorResponse, HttpResponse]]
      )
      .value
      .collect {
        case Right(x: HttpResponse) if x.status == NO_CONTENT              => Right(Done)
        case Left(x: UpstreamErrorResponse) if x.statusCode == BAD_REQUEST =>
          Left(UserAnswersErrorResponse(x.message, Some("Payload received is invalid")))
        case Left(x: UpstreamErrorResponse)                                =>
          logger.warn(s"[UserAnswersConnector][putAnswers] Downstream error statusCode: ${x.statusCode}")
          Left(UserAnswersErrorResponse(x.message, None))
      }

  def postSubmission(submissionDTO: SubmissionDTO, srnNumber: SrnNumber)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[SubmissionType] =
    httpClientResponse
      .read(
        http
          .post(submissionUrl(submissionDTO.referenceId.value))
          .setHeader("schemeReferenceNumber" -> srnNumber.value)
          .withBody(Json.toJson(submissionDTO))
          .execute[Either[UpstreamErrorResponse, HttpResponse]]
      )
      .value
      .collect {
        case Right(x: HttpResponse) if x.status == OK =>
          x.json.validate[SubmissionResponse] match {
            case JsSuccess(value, _) => Right(value)
            case JsError(errors)     =>
              val formatted = formatJsonErrors(errors)
              logger.warn(
                s"[SubmissionConnector][postSubmission] Unable to parse Json as SubmissionResponse: $formatted"
              )
              Left(SubmissionErrorResponse("Unable to parse Json as SubmissionResponse", Some(formatted)))
          }
        case Left(x: UpstreamErrorResponse)           => Left(SubmissionErrorResponse(x.message, None))

      }

  private def submissionUrl(id: String): URL =
    url"${appConfig.backendService}/submit-declaration/$id"

  def deleteAnswers(id: String, srnNumber: SrnNumber)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[DeleteUserAnswersType] = {
    def url: URL = url"${appConfig.backendService}/save-for-later/$id"

    httpClientResponse
      .read(
        http
          .delete(url)
          .setHeader("schemeReferenceNumber" -> srnNumber.value)
          .execute[Either[UpstreamErrorResponse, HttpResponse]]
      )
      .value
      .collect {
        case Right(x: HttpResponse) if x.status == NO_CONTENT => Right(Done)
        case Left(x: UpstreamErrorResponse)                   =>
          logger.warn(
            s"[UserAnswersConnector][deleteAnswers] Error returned: downstreamStatus: ${x.statusCode}, error: ${x.message}"
          )
          Left(UserAnswersErrorResponse(x.message, None))
      }
  }

  def resetDatabase(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    val url = url"${appConfig.backendHost}/test-only/reset-test-data"
    http
      .delete(url)
      .execute[HttpResponse]
  }
}
