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
import models.authentication.{PsaId, PspId}
import models.{IndividualDetails, MinimalDetails}
import play.api.libs.json.Json
import play.api.test.Injecting
import stubs.MinimalDetailsStub

import scala.concurrent.ExecutionContext.Implicits.global

class MinimalDetailsConnectorISpec extends BaseISpec with Injecting {

  val connector: MinimalDetailsConnector = inject[MinimalDetailsConnector]

  private val psaId = PsaId("A2100005")
  private val pspId = PspId("21000005")

  val minimalDetails: MinimalDetails =
    MinimalDetails(
      email = "test.user@example.com",
      isPsaSuspended = false,
      organisationName = None,
      individualDetails = Some(
        IndividualDetails(
          firstName = "Test",
          middleName = Some("Middle"),
          lastName = "User"
        )
      ),
      rlsFlag = false,
      deceasedFlag = false
    )

  "MinimalDetailsConnector.fetch" when {
    "fetching minimal details" must {
      "return Right(MinimalDetails) for PSA route" in {
        MinimalDetailsStub.psaSuccess(psaId.value, Json.stringify(Json.toJson(minimalDetails)))

        await(connector.fetch(psaId)) shouldBe Right(minimalDetails)
      }

      "return Right(MinimalDetails) for PSP route" in {
        MinimalDetailsStub.pspSuccess(pspId.value, Json.stringify(Json.toJson(minimalDetails)))

        await(connector.fetch(pspId)) shouldBe Right(minimalDetails)
      }

      "return Left(DetailsNotFound) when 404 returned containing 'no match found'" in {
        MinimalDetailsStub.psaNotFoundWithNoMatch(psaId.value)

        await(connector.fetch(psaId)) shouldBe Left(DetailsNotFound)
      }

      "return Left(UpstreamError) when other upstream errors occur" in {
        MinimalDetailsStub.psaForbiddenDelimited(psaId.value)

        await(connector.fetch(psaId)) shouldBe Left(UpstreamError)
      }
    }
  }
}
