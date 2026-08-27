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

import base.BaseISpec
import models.QtStatus.Submitted
import models.responses.*
import models.{PstrNumber, QtNumber, SrnNumber}
import org.scalatest.OptionValues
import play.api.test.Injecting
import stubs.TransferBackendStub

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global

class TransferConnectorISpec extends BaseISpec with Injecting with OptionValues {

  private val connector: TransferConnector = inject[TransferConnector]

  private val pstr = PstrNumber("12345678AB")
  private val srn  = SrnNumber("1234567890")

  "TransferConnector.getAllTransfers" when {

    "the backend returns 200" must {
      val testNino = generateNino()

      "return Right(GetAllTransfersDTO) with the parsed transfers if payload valid" in {
        TransferBackendStub.stubGetAllTransfersOk(pstr.value, testNino)

        val result = await(connector.getAllTransfers(srn, pstr))

        result match {
          case Right(dto) =>
            dto.pstr.value         shouldBe pstr.value
            dto.transfers.nonEmpty shouldBe true

            val first = dto.transfers.head
            first.transferId            shouldBe QtNumber("QT564321")
            first.qtVersion.value       shouldBe "001"
            first.memberFirstName.value shouldBe "David"
            first.memberSurname.value   shouldBe "Warne"
            first.nino.value            shouldBe testNino
            first.submissionDate.value  shouldBe Instant.parse("2025-03-14T00:00:00Z")
            first.lastUpdated           shouldBe empty
            first.qtStatus.value        shouldBe Submitted
            first.pstrNumber.value      shouldBe pstr

          case Left(err) =>
            fail(s"Expected Right(dto) but got $err")
        }
      }

      "drop the invalid items and keep only valid ones if some of payload is invalid" in {
        // 3 items: 1 valid submitted, 1 valid in-progress, 1 invalid (has both dates)
        TransferBackendStub.stubGetAllTransfersOkWithInvalidItems(pstr.value, testNino)

        val result = await(connector.getAllTransfers(srn, pstr))

        result match {
          case Right(dto) =>
            dto.transfers.size                   shouldBe 2
            all(dto.transfers.map(_.isValid))    shouldBe true
            all(dto.transfers.map(_.nino.value)) shouldBe testNino

          case Left(err) =>
            fail(s"Expected Right(dto) but got $err")
        }
      }

      "map to Left(AllTransfersUnexpectedError) if json malformed" in {
        TransferBackendStub.stubGetAllTransfersMalformed(pstr.value)

        val result = await(connector.getAllTransfers(srn, pstr))

        result match {
          case Left(_: AllTransfersUnexpectedError) => succeed
          case other                                => fail(s"Expected AllTransfersUnexpectedError but got $other")
        }
      }
    }

    "the backend returns 404" must {
      "map to Left(NoTransfersFound)" in {
        TransferBackendStub.stubGetAllTransfersNotFound(pstr.value)

        val result = await(connector.getAllTransfers(srn, pstr))

        result shouldBe Left(NoTransfersFound)
      }
    }

    "the backend returns 500" must {
      "map to Left(InternalServerError)" in {
        TransferBackendStub.stubGetAllTransfersServerError(pstr.value)

        val result = await(connector.getAllTransfers(srn, pstr))

        result shouldBe Left(InternalServerError)
      }
    }
  }
}
