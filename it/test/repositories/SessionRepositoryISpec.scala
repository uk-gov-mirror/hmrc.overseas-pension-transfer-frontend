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

package repositories

import base.SpecBase
import config.FrontendAppConfig
import models.authentication.{PsaId, PsaUser}
import models.{PensionSchemeDetails, PstrNumber, SessionData, SrnNumber}
import org.mockito.Mockito.when
import org.mongodb.scala.ObservableFuture
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.model.Filters
import org.scalactic.source.Position
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.slf4j.MDC
import play.api.libs.json.{JsObject, Json}
import services.EncryptionService
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import uk.gov.hmrc.mdc.MdcExecutionContext
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.concurrent.{ExecutionContext, Future}

trait SessionRepositoryISpec(protected val isEncrypted: Boolean)
    extends AnyFreeSpec
    with Matchers
    with DefaultPlayMongoRepositorySupport[SessionData]
    with ScalaFutures
    with IntegrationPatience
    with OptionValues
    with MockitoSugar
    with SpecBase {

  protected val instant: Instant = now.truncatedTo(ChronoUnit.MILLIS)

  protected val sessionData = SessionData(
    "id",
    userAnswersTransferNumber,
    PensionSchemeDetails(
      SrnNumber("1234567890123"),
      PstrNumber("12345678AB"),
      "Scheme Name"
    ),
    PsaUser(
      PsaId("A123456"),
      "internalId",
      Individual
    ),
    Json.obj("foo" -> "bar"),
    Instant.ofEpochSecond(1)
  )

  protected val mockAppConfig: FrontendAppConfig = mock[FrontendAppConfig]
  when(mockAppConfig.cacheTtl) thenReturn 1L
  when(mockAppConfig.mongoDBEncryption) thenReturn isEncrypted

  implicit val productionLikeTestMdcExecutionContext: ExecutionContext = MdcExecutionContext()

  protected val encryptionService = new EncryptionService("test-master-key")

  override protected val repository: SessionRepository

  override protected def beforeEach(): Unit = {
    deleteAll()
    ()
  }

  // If it can read data node as a String then it is encrypted, otherwise it will be JsObject (unencrypted).
  private def isStoredValueEncrypted: Boolean = {
    val jsObj = Json
      .parse(
        repository.collection
          .find[BsonDocument](Filters.equal("_id", sessionData.sessionId))
          .toFuture()
          .futureValue
          .headOption
          .value
          .toJson
      )
      .as[JsObject]
    (jsObj \ "data").toOption.flatMap(_.asOpt[String]).isDefined
  }

  private def encryptedMessage: String = if (isEncrypted) "when encryption is on " else " when encryption is off "

  s".set $encryptedMessage" - {

    "must set the last updated time on the supplied user answers to `now`, and save them" in {
      val expectedResult = sessionData copy (lastUpdated = instant)

      repository.set(sessionData).futureValue
      val updatedRecord = find(Filters.equal("_id", sessionData.sessionId)).futureValue.headOption.value

      isStoredValueEncrypted mustBe isEncrypted
      updatedRecord mustEqual expectedResult
    }

    mustPreserveMdc(repository.set(sessionData))
  }

  s".get $encryptedMessage" - {

    "when there is a record for this id" - {

      "must update the lastUpdated time and get the record" in {

        insert(sessionData).futureValue

        val result         = repository.get(sessionData.sessionId).futureValue
        val expectedResult = sessionData copy (lastUpdated = instant)
        isStoredValueEncrypted mustBe isEncrypted
        result.value mustEqual expectedResult
      }
    }

    "when there is no record for this id" - {

      "must return None" in {

        repository.get("id that does not exist").futureValue must not be defined
      }
    }

    mustPreserveMdc(repository.get(sessionData.sessionId))
  }

  s".clear $encryptedMessage" - {

    "must remove a record" in {

      insert(sessionData).futureValue
      isStoredValueEncrypted mustBe isEncrypted
      repository.clear(sessionData.transferId.value).futureValue

      repository.get(sessionData.transferId.value).futureValue must not be defined
    }

    "must return true when there is no record to remove" in {
      val result = repository.clear("id that does not exist").futureValue

      result mustEqual true
    }

    mustPreserveMdc(repository.clear(sessionData.transferId.value))
  }

  s".keepAlive $encryptedMessage" - {

    "when there is a record for this id" - {

      "must update its lastUpdated to `now` and return true" in {
        insert(sessionData).futureValue

        val result         = repository.keepAlive(sessionData.sessionId).futureValue
        result mustBe true
        isStoredValueEncrypted mustBe isEncrypted
        val updatedAnswers = find(Filters.equal("_id", sessionData.sessionId)).futureValue.headOption.value

        updatedAnswers.lastUpdated mustEqual instant
        updatedAnswers.copy(lastUpdated = sessionData.lastUpdated) mustEqual sessionData
      }
    }

    "when there is no record for this id" - {
      "must return true" in {
        val result = repository.keepAlive("id that does not exist").futureValue
        result mustBe true
      }
    }

    mustPreserveMdc(repository.keepAlive(sessionData.sessionId))
  }

  private def mustPreserveMdc[A](f: => Future[A])(implicit pos: Position): Unit =
    "must preserve MDC" in {

      MDC.put("test", "foo")

      f.map { _ =>
        Option(MDC.get("test"))
      }.futureValue mustEqual Some("foo")
    }
}

class SessionRepositoryEncryptionToggledOnISpec extends SessionRepositoryISpec(true) {
  override protected val repository: SessionRepository =
    new SessionRepository(
      mongoComponent = mongoComponent,
      encryptionService = encryptionService,
      appConfig = mockAppConfig,
      clock = clock
    )
}

class SessionRepositoryEncryptionToggledOffISpec extends SessionRepositoryISpec(false) {
  override protected val repository: SessionRepository =
    new SessionRepository(
      mongoComponent = mongoComponent,
      encryptionService = encryptionService,
      appConfig = mockAppConfig,
      clock = clock
    )
}
