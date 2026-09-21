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

package stubs

import base.SpecBase
import com.github.tomakehurst.wiremock.client.WireMock.*
import models.TransferNumber

import java.util.UUID

object TransferBackendStub extends SpecBase {

  val transferNumber: TransferNumber = TransferNumber(UUID.randomUUID().toString)

  private def allTransfersUrl(pstr: String) =
    s"/overseas-pension-transfer-backend/get-all-transfers/$pstr"

  private def specificUrl(referenceId: String) =
    s"/overseas-pension-transfer-backend/get-transfer/$referenceId"

  // ----- get all -----

  def stubGetAllTransfersOk(pstr: String, nino: String): Unit = {
    stubFor(
      get(urlEqualTo(allTransfersUrl(pstr)))
        .willReturn(okJson(successJson(pstr, nino)))
    )
    ()
  }

  def stubGetAllTransfersOkWithInvalidItems(pstr: String, nino: String): Unit = {
    stubFor(
      get(urlEqualTo(allTransfersUrl(pstr)))
        .willReturn(okJson(successJsonWithInvalidItem(pstr, nino)))
    )
    ()
  }

  def stubGetAllTransfersNotFound(pstr: String): Unit = {
    stubFor(
      get(urlEqualTo(allTransfersUrl(pstr)))
        .willReturn(notFound())
    )
    ()
  }

  def stubGetAllTransfersServerError(pstr: String): Unit = {
    stubFor(
      get(urlEqualTo(allTransfersUrl(pstr)))
        .willReturn(serverError())
    )
    ()
  }

  def stubGetAllTransfersMalformed(pstr: String): Unit = {
    stubFor(
      get(urlEqualTo(allTransfersUrl(pstr)))
        .willReturn(okJson("""{ "this": "is-not-a-valid-dto" }"""))
    )
    ()
  }

  // ----- JSON fixtures (get all) -----

  private def successJson(pstr: String, nino: String): String =
    s"""
       |{
       |  "pstr": "$pstr",
       |  "lastUpdated": "2025-09-24T10:00:00Z",
       |  "transfers": [
       |    {
       |      "transferId": "QT564321",
       |      "qtVersion": "001",
       |      "nino": "$nino",
       |      "memberFirstName": "David",
       |      "memberSurname": "Warne",
       |      "submissionDate": "2025-03-14T00:00:00Z",
       |      "qtStatus": "Submitted",
       |      "pstrNumber": "$pstr"
       |    },
       |    {
       |      "transferId": "QT564322",
       |      "qtVersion": "003",
       |      "nino": "$nino",
       |      "memberFirstName": "Edith",
       |      "memberSurname": "Ennis-Hill",
       |      "lastUpdated": "2025-05-01T00:00:00Z",
       |      "qtStatus": "InProgress",
       |      "pstrNumber": "$pstr"
       |    }
       |  ]
       |}
       |""".stripMargin

  /** Three items: 2 valid, 1 invalid (has both dates -> should be dropped) */
  private def successJsonWithInvalidItem(pstr: String, nino: String): String =
    s"""
       |{
       |  "pstr": "$pstr",
       |  "lastUpdated": "2025-09-24T10:00:00Z",
       |  "transfers": [
       |    {
       |      "transferId": "${transferNumber.value}",
       |      "nino": "$nino",
       |      "memberFirstName": "Alice",
       |      "memberSurname": "Adams",
       |      "submissionDate": "2025-01-10T00:00:00Z",
       |      "qtStatus": "Submitted",
       |      "pstrNumber": "$pstr"
       |    },
       |    {
       |      "transferId": "${transferNumber.value}",
       |      "nino": "$nino",
       |      "memberFirstName": "Bob",
       |      "memberSurname": "Brown",
       |      "lastUpdated": "2025-02-11T00:00:00Z",
       |      "qtStatus": "InProgress",
       |      "pstrNumber": "$pstr"
       |    },
       |    {
       |      "transferId": "${transferNumber.value}",
       |      "nino": "$nino",
       |      "memberFirstName": "Charlie",
       |      "memberSurname": "Clark",
       |      "submissionDate": "2025-03-12T00:00:00Z",
       |      "lastUpdated": "2025-03-13T00:00:00Z",
       |      "qtStatus": "Submitted",
       |      "pstrNumber": "$pstr"
       |    }
       |  ]
       |}
       |""".stripMargin

  // ----- get specific -----

  def stubGetSpecificTransferOk(
    referenceId: String,
    pstr: String,
    qtStatus: String,
    dataJson: String,
    lastUpdatedIso: String,
    versionNumber: Option[String] = None
  ): Unit = {
    val base =
      get(urlPathEqualTo(specificUrl(referenceId)))
        .withQueryParam("pstr", equalTo(pstr))
        .withQueryParam("qtStatus", equalTo(qtStatus))

    val withVersion =
      versionNumber.fold(base)(v => base.withQueryParam("versionNumber", equalTo(v)))

    stubFor(
      withVersion.willReturn(
        okJson(
          s"""
             |{
             |  "transferId": "$referenceId",
             |  "pstr": "$pstr",
             |  "data": $dataJson,
             |  "lastUpdated": "$lastUpdatedIso"
             |}
             |""".stripMargin
        )
      )
    )
    ()
  }

  def stubGetSpecificTransferMalformed(
    referenceId: String,
    pstr: String,
    qtStatus: String,
    versionNumber: Option[String] = None
  ): Unit = {
    val base =
      get(urlPathEqualTo(specificUrl(referenceId)))
        .withQueryParam("pstr", equalTo(pstr))
        .withQueryParam("qtStatus", equalTo(qtStatus))

    val withVersion =
      versionNumber.fold(base)(v => base.withQueryParam("versionNumber", equalTo(v)))

    stubFor(
      withVersion.willReturn(okJson("""{ "bad": "shape" }"""))
    )
    ()
  }

  def stubGetSpecificTransferNotFound(
    referenceId: String,
    pstr: String,
    qtStatus: String,
    versionNumber: Option[String] = None
  ): Unit = {
    val base =
      get(urlPathEqualTo(specificUrl(referenceId)))
        .withQueryParam("pstr", equalTo(pstr))
        .withQueryParam("qtStatus", equalTo(qtStatus))

    val withVersion =
      versionNumber.fold(base)(v => base.withQueryParam("versionNumber", equalTo(v)))

    stubFor(
      withVersion.willReturn(notFound())
    )
    ()
  }

  def stubGetSpecificTransferServerError(
    referenceId: String,
    pstr: String,
    qtStatus: String,
    versionNumber: Option[String] = None
  ): Unit = {
    val base =
      get(urlPathEqualTo(specificUrl(referenceId)))
        .withQueryParam("pstr", equalTo(pstr))
        .withQueryParam("qtStatus", equalTo(qtStatus))

    val withVersion =
      versionNumber.fold(base)(v => base.withQueryParam("versionNumber", equalTo(v)))

    stubFor(
      withVersion.willReturn(serverError())
    )
    ()
  }
}
