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

package models

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.*
import queries.{Gettable, Settable}
import services.EncryptionService
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant
import scala.util.{Failure, Success, Try}

final case class DashboardData(
  id: String,
  data: JsObject,
  lastUpdated: Instant
) {

  def get[A](page: Gettable[A])(implicit rds: Reads[A]): Option[A] =
    Reads.optionNoError(Reads.at(page.path)).reads(data).getOrElse(None)

  def set[A](page: Settable[A], value: A)(implicit writes: Writes[A]): Try[DashboardData] = {

    val updatedData = data.setObject(page.path, Json.toJson(value)) match {
      case JsSuccess(jsValue, _) =>
        Success(jsValue)
      case JsError(errors)       =>
        Failure(JsResultException(errors))
    }

    updatedData.flatMap { d =>
      Success(copy(data = d))
    }
  }

  def remove[A](page: Settable[A]): Try[DashboardData] = {

    val updatedData = data.removeObject(page.path) match {
      case JsSuccess(jsValue, _) =>
        Success(jsValue)
      case JsError(_)            =>
        Success(data)
    }

    updatedData.flatMap { d =>
      Success(copy(data = d))
    }
  }
}

object DashboardData {

  val reads: Reads[DashboardData] =
    (
      (__ \ "_id").read[String] and
        (__ \ "data").read[JsObject] and
        (__ \ "lastUpdated").read(MongoJavatimeFormats.instantFormat)
    )(DashboardData.apply _)

  val writes: OWrites[DashboardData] =
    (
      (__ \ "_id").write[String] and
        (__ \ "data").write[JsObject] and
        (__ \ "lastUpdated").write(MongoJavatimeFormats.instantFormat)
    )(ua => (ua.id, ua.data, ua.lastUpdated))

  implicit val format: OFormat[DashboardData] = OFormat(reads, writes)

  sealed trait DashboardDataWrapper

  final case class EncryptedDashboardData(encryptedString: String) extends DashboardDataWrapper {

    def decrypt(implicit encryptionService: EncryptionService): Either[Throwable, JsObject] =
      encryptionService.decrypt(encryptedString).map(json => Json.parse(json).as[JsObject])
  }

  final case class DecryptedDashboardData(data: JsObject) extends DashboardDataWrapper {

    def encrypt(implicit encryptionService: EncryptionService): EncryptedDashboardData =
      EncryptedDashboardData(encryptionService.encrypt(Json.stringify(data)))
  }

  object DashboardDataWrapper {
    implicit val encryptedFormat: OFormat[EncryptedDashboardData] = Json.format[EncryptedDashboardData]
    implicit val decryptedFormat: OFormat[DecryptedDashboardData] = Json.format[DecryptedDashboardData]
  }

  def encryptedFormat(encryptionService: EncryptionService): OFormat[DashboardData] = {

    val reads: Reads[DashboardData] = (
      (__ \ "_id").read[String] and
        (__ \ "data").read[String].map { enc =>
          EncryptedDashboardData(enc).decrypt(encryptionService) match {
            case Right(decryptedJs) => decryptedJs
            case Left(err)          => throw new RuntimeException(s"Decryption failed: ${err.getMessage}", err)
          }
        } and
        (__ \ "lastUpdated").read(MongoJavatimeFormats.instantFormat)
    )(DashboardData.apply _)

    val writes: OWrites[DashboardData] = OWrites { dd =>
      implicit val es: EncryptionService            = encryptionService
      val encrypted: EncryptedDashboardData         = DecryptedDashboardData(dd.data).encrypt
      Json.obj(
        "_id"         -> dd.id,
        "data"        -> encrypted.encryptedString,
        "lastUpdated" -> MongoJavatimeFormats.instantFormat.writes(dd.lastUpdated)
      )
    }

    OFormat(reads, writes)
  }

  def unencryptedFormat: OFormat[DashboardData] = {
    val reads: Reads[DashboardData] = (
      (__ \ "_id").read[String] and
        (__ \ "data").read[JsObject] and
        (__ \ "lastUpdated").read(MongoJavatimeFormats.instantFormat)
    )(DashboardData.apply _)

    val writes: OWrites[DashboardData] = OWrites { dd =>
      Json.obj(
        "_id"         -> dd.id,
        "data"        -> dd.data,
        "lastUpdated" -> MongoJavatimeFormats.instantFormat.writes(dd.lastUpdated)
      )
    }

    OFormat(reads, writes)
  }

  def create(id: String, now: Instant): DashboardData = DashboardData(
    id = id,
    data = Json.obj(),
    lastUpdated = now
  )

  def empty(now: Instant): DashboardData = create("unknown", now)
}
