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

package models

import models.authentication.AuthenticatedUser
import play.api.libs.functional.syntax.*
import play.api.libs.json.*
import queries.{Gettable, Settable}
import services.EncryptionService
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant
import scala.util.{Failure, Success, Try}

case class SessionData(
  sessionId: String,
  transferId: TransferId,
  schemeInformation: PensionSchemeDetails,
  user: AuthenticatedUser,
  data: JsObject,
  lastUpdated: Instant
) {

  def get[A](page: Gettable[A])(implicit rds: Reads[A]): Option[A] =
    Reads.optionNoError(Reads.at(page.path)).reads(data).getOrElse(None)

  import play.api.libs.json.*

  def set[A](page: Settable[A], value: A)(implicit writes: Writes[A]): Try[SessionData] = {

    val updatedData = data.setObject(page.path, Json.toJson(value)) match {
      case JsSuccess(jsValue, _) =>
        Success(jsValue)
      case JsError(errors)       =>
        Failure(JsResultException(errors))
    }

    updatedData.flatMap { d =>
      val updatedSession = copy(data = d)
      Success(updatedSession)
    }
  }

  def remove[A](page: Settable[A]): Try[SessionData] = {

    val updatedData = data.removeObject(page.path) match {
      case JsSuccess(jsValue, _) =>
        Success(jsValue)
      case JsError(_)            =>
        Success(data)
    }

    updatedData.flatMap { d =>
      val updatedSession = copy(data = d)
      Success(updatedSession)
    }
  }
}

object SessionData {

  implicit val reads: Reads[SessionData] = (
    (__ \ "_id").read[String] and
      (__ \ "transferId").read[TransferId] and
      (__ \ "schemeInformation").read[PensionSchemeDetails] and
      (__ \ "user").read[AuthenticatedUser] and
      (__ \ "data").read[JsObject] and
      (__ \ "lastUpdated").read(MongoJavatimeFormats.instantFormat)
  )(SessionData.apply _)

  implicit val writes: Writes[SessionData] = (
    (__ \ "_id").write[String] and
      (__ \ "transferId").write[TransferId] and
      (__ \ "schemeInformation").write[PensionSchemeDetails] and
      (__ \ "user").write[AuthenticatedUser] and
      (__ \ "data").write[JsObject] and
      (__ \ "lastUpdated").write(MongoJavatimeFormats.instantWrites)
  )(session =>
    (session.sessionId, session.transferId, session.schemeInformation, session.user, session.data, session.lastUpdated)
  )

  implicit val format: Format[SessionData] = Format[SessionData](reads, writes)

  sealed trait SessionDataWrapper

  final case class EncryptedSessionData(encryptedString: String) extends SessionDataWrapper {

    def decrypt(implicit encryptionService: EncryptionService): Either[Throwable, JsObject] =
      encryptionService.decrypt(encryptedString).map { json =>
        Json.parse(json).as[JsObject]
      }
  }

  final case class DecryptedSessionData(data: JsObject) extends SessionDataWrapper {

    def encrypt(implicit encryptionService: EncryptionService): EncryptedSessionData =
      EncryptedSessionData(encryptionService.encrypt(Json.stringify(data)))
  }

  object SessionDataWrapper {
    implicit val encryptedFormat: OFormat[EncryptedSessionData] = Json.format[EncryptedSessionData]
    implicit val decryptedFormat: OFormat[DecryptedSessionData] = Json.format[DecryptedSessionData]
  }

  def encryptedFormat(encryptionService: EncryptionService): OFormat[SessionData] = {

    val reads: Reads[SessionData] = (
      (__ \ "_id").read[String] and
        (__ \ "transferId").read[TransferId] and
        (__ \ "schemeInformation").read[PensionSchemeDetails] and
        (__ \ "user").read[AuthenticatedUser] and
        (__ \ "data").read[String].map { enc =>
          EncryptedSessionData(enc).decrypt(encryptionService) match {
            case Right(decryptedJs) =>
              decryptedJs
            case Left(err)          =>
              // fail fast: decryption failure should be surfaced loudly in logs
              throw new RuntimeException(s"Decryption failed: ${err.getMessage}", err)
          }
        } and
        (__ \ "lastUpdated").read(MongoJavatimeFormats.instantFormat)
    )(SessionData.apply _)

    val writes: OWrites[SessionData] = OWrites { sd =>
      implicit val es: EncryptionService                = encryptionService
      val encrypted: EncryptedSessionData               = DecryptedSessionData(sd.data).encrypt
      Json.obj(
        "_id"               -> sd.sessionId,
        "transferId"        -> sd.transferId,
        "schemeInformation" -> Json.toJson(sd.schemeInformation),
        "user"              -> Json.toJson(sd.user),
        "data"              -> encrypted.encryptedString,
        "lastUpdated"       -> MongoJavatimeFormats.instantFormat.writes(sd.lastUpdated)
      )
    }

    OFormat(reads, writes)
  }

  def unencryptedFormat: OFormat[SessionData] = {

    val reads: Reads[SessionData] = (
      (__ \ "_id").read[String] and
        (__ \ "transferId").read[TransferId] and
        (__ \ "schemeInformation").read[PensionSchemeDetails] and
        (__ \ "user").read[AuthenticatedUser] and
        (__ \ "data").read[JsObject] and
        (__ \ "lastUpdated").read(MongoJavatimeFormats.instantFormat)
    )(SessionData.apply _)

    val writes: OWrites[SessionData] = OWrites { sd =>
      Json.obj(
        "_id"               -> sd.sessionId,
        "transferId"        -> sd.transferId,
        "schemeInformation" -> Json.toJson(sd.schemeInformation),
        "user"              -> Json.toJson(sd.user),
        "data"              -> Json.toJson(sd.data),
        "lastUpdated"       -> MongoJavatimeFormats.instantFormat.writes(sd.lastUpdated)
      )
    }

    OFormat(reads, writes)
  }
}
