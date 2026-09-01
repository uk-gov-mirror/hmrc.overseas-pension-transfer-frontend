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

import base.SpecBase
import models.audit.JourneyStartedType.{StartJourneyFailed, StartNewTransfer}
import models.audit.ReportStartedAuditModel
import models.authentication.{PsaId, PsaUser}
import models.{AllTransfersItem, TransferId}
import org.mockito.ArgumentMatchers.{any, argThat, eq as eqTo}
import org.mockito.Mockito.*
import org.scalatest.BeforeAndAfterEach
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import repositories.EnhancedLockRepository
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.lock.Lock

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class LockServiceSpec extends AnyFreeSpec with Matchers with SpecBase with MockitoSugar with BeforeAndAfterEach {

  implicit private val hc: HeaderCarrier = HeaderCarrier()

  private val mockEnhancedLockRepository = mock[EnhancedLockRepository]
  private val mockAuditService           = mock[AuditService]

  private val service = new LockService(mockEnhancedLockRepository, mockAuditService)

  private val transferId        = TransferId("QT123456")
  private val owner             = "test-owner"
  private val ttlSeconds        = 120L
  private val authenticatedUser = PsaUser(PsaId("A1234567"), "internal-123", AffinityGroup.Organisation)

  private val allTransfersItem = Some(
    AllTransfersItem(
      transferId = transferId,
      qtVersion = Some("001"),
      qtStatus = None,
      nino = Some("AA123456A"),
      memberFirstName = Some("John"),
      memberSurname = Some("Doe"),
      qtDate = None,
      lastUpdated = Some(now),
      pstrNumber = None,
      submissionDate = None
    )
  )

  override def beforeEach(): Unit =
    reset(mockEnhancedLockRepository, mockAuditService)

  "LockService" - {

    "must acquire lock successfully and trigger StartJourney audit" in {
      val fakeLock = Lock(transferId.value, owner, now, now.plusSeconds(60))
      when(mockEnhancedLockRepository.takeLock(eqTo(transferId.value), eqTo(owner), any[Duration]))
        .thenReturn(Future.successful(Some(fakeLock)))

      val result = await(
        service.takeLockWithAudit(
          transferId,
          owner,
          ttlSeconds,
          authenticatedUser,
          schemeDetails,
          StartNewTransfer,
          allTransfersItem
        )
      )

      result mustBe true
      verify(mockAuditService).audit(any[ReportStartedAuditModel])(any[HeaderCarrier])
      verify(mockEnhancedLockRepository).takeLock(eqTo(transferId.value), eqTo(owner), any[Duration])
      verifyNoMoreInteractions(mockAuditService)
    }

    "must return false and trigger StartJourneyFailed audit when lock is already taken" in {
      when(mockEnhancedLockRepository.takeLock(eqTo(transferId.value), eqTo(owner), any()))
        .thenReturn(Future.successful(None))

      val result = await(
        service.takeLockWithAudit(
          transferId,
          owner,
          ttlSeconds,
          authenticatedUser,
          schemeDetails,
          StartNewTransfer,
          allTransfersItem
        )
      )

      result mustBe false
      verify(mockAuditService)
        .audit(argThat[ReportStartedAuditModel](_.journeyType == StartJourneyFailed))(any[HeaderCarrier])
      verify(mockEnhancedLockRepository).takeLock(eqTo(transferId.value), eqTo(owner), any())
      verifyNoMoreInteractions(mockAuditService)
    }

    "must acquire and release lock using simple takeLock and releaseLock methods" in {
      val fakeLock = Lock("lock1", owner, now, now.plusSeconds(60))
      when(mockEnhancedLockRepository.takeLock(eqTo("lock1"), eqTo(owner), any()))
        .thenReturn(Future.successful(Some(fakeLock)))
      when(mockEnhancedLockRepository.releaseLock(eqTo("lock1"), eqTo(owner)))
        .thenReturn(Future.unit)

      val takeLockResult = await(service.takeLock("lock1", owner, ttlSeconds))
      takeLockResult mustBe true

      await(service.releaseLock("lock1", owner))

      verify(mockEnhancedLockRepository).takeLock(eqTo("lock1"), eqTo(owner), any())
      verify(mockEnhancedLockRepository).releaseLock(eqTo("lock1"), eqTo(owner))
    }

    "must return false if simple takeLock fails" in {
      when(mockEnhancedLockRepository.takeLock(eqTo("lock2"), eqTo(owner), any()))
        .thenReturn(Future.successful(None))

      val result = await(service.takeLock("lock2", owner, ttlSeconds))
      result mustBe false

      verify(mockEnhancedLockRepository).takeLock(eqTo("lock2"), eqTo(owner), any())
    }

    "must return true when lock is currently locked by the owner (isLocked)" in {
      when(mockEnhancedLockRepository.isLocked(eqTo("lock1"), eqTo(owner)))
        .thenReturn(Future.successful(true))

      val result = await(service.isLocked("lock1", owner))
      result mustBe true

      verify(mockEnhancedLockRepository).isLocked(eqTo("lock1"), eqTo(owner))
    }

    "must return false when lock is not held by the owner (isLocked)" in {
      when(mockEnhancedLockRepository.isLocked(eqTo("lock2"), eqTo(owner)))
        .thenReturn(Future.successful(false))

      val result = await(service.isLocked("lock2", owner))
      result mustBe false

      verify(mockEnhancedLockRepository).isLocked(eqTo("lock2"), eqTo(owner))
    }

  }

  private def await[T](f: Future[T]): T =
    Await.result(f, scala.concurrent.duration.Duration.Inf)
}
