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

package services

import com.google.inject.Inject
import connectors.UserAnswersConnector
import models.*
import models.QtStatus.Submitted
import models.dtos.UserAnswersDTO.toUserAnswers
import models.responses.*
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class CollectSubmittedVersionsService @Inject() (
  userAnswersConnector: UserAnswersConnector
)(implicit ec: ExecutionContext) {

  def collectVersions(
    qtReference: TransferId,
    pstr: PstrNumber,
    qtStatus: QtStatus,
    versionNumber: String,
    srnNumber: SrnNumber
  )(implicit hc: HeaderCarrier): Future[(Option[UserAnswers], List[UserAnswers])] = {

    def findDraft: Future[Option[UserAnswers]] = userAnswersConnector.getAnswers(qtReference.value, srnNumber).map {
      case Right(dto) => Some(toUserAnswers(dto))
      case Left(_)    => None
    }

    def collectVersions: Future[List[UserAnswers]] =
      if (versionNumber == "001") {
        userAnswersConnector.getAnswers(qtReference, pstr, Submitted, Some(versionNumber), srnNumber) map {
          case Right(dto)                        => List(toUserAnswers(dto))
          case Left(UserAnswersNotFoundResponse) =>
            Nil
          case Left(ex)                          =>
            throw new RuntimeException(ex.toString)
        }
      } else {
        val versions = (1 to versionNumber.toInt).toList

        versions.foldLeft(Future.successful(List[UserAnswers]())) { case (acc, version) =>
          val stringifyVersion = version.toString.length match {
            case 1 => s"00$version"
            case 2 => s"0$version"
            case _ => version.toString
          }
          userAnswersConnector.getAnswers(qtReference, pstr, qtStatus, Some(stringifyVersion), srnNumber) flatMap {
            case Right(dto)                        =>
              acc.map(currentList => toUserAnswers(dto) :: currentList)
            case Left(UserAnswersNotFoundResponse) => acc
            case Left(ex)                          =>
              throw new RuntimeException(ex.toString)
          }
        }
      }

    for {
      maybeDraft  <- findDraft
      versionList <- collectVersions
    } yield (maybeDraft, versionList)
  }
}
