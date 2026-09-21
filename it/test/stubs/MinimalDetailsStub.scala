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

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.stubbing.StubMapping

object MinimalDetailsStub {

  private val PathRegex = ".*/pension-administrator/get-minimal-details-self"

  private def withHeaders(
    builder: com.github.tomakehurst.wiremock.client.MappingBuilder,
    headers: Seq[(String, String)]
  ) =
    headers.foldLeft(builder) { case (b, (k, v)) => b.withHeader(k, equalTo(v)) }

  private def stubGetWithHeaders(
    status: Int,
    body: String,
    requiredHeaders: Seq[(String, String)]
  ): StubMapping = {
    val builder = get(urlPathMatching(PathRegex)).atPriority(1)
    val withAll = withHeaders(builder, requiredHeaders)

    stubFor(
      withAll.willReturn(
        aResponse()
          .withStatus(status)
          .withBody(body)
          .withHeader("Content-Type", "application/json")
      )
    )
  }

  def psaSuccess(psaId: String, body: String): Unit = {
    stubGetWithHeaders(
      status = 200,
      body = body,
      requiredHeaders = Seq("psaId" -> psaId, "loggedInAsPsa" -> "true")
    )
    ()
  }

  def pspSuccess(pspId: String, body: String): Unit = {
    stubGetWithHeaders(
      status = 200,
      body = body,
      requiredHeaders = Seq("pspId" -> pspId, "loggedInAsPsa" -> "false")
    )
    ()
  }

  def psaNotFoundWithNoMatch(psaId: String): Unit = {
    stubGetWithHeaders(
      status = 404,
      body = "no match found",
      requiredHeaders = Seq("psaId" -> psaId, "loggedInAsPsa" -> "true")
    )
    ()
  }

  def psaForbiddenDelimited(psaId: String): Unit = {
    stubGetWithHeaders(
      status = 403,
      body = "DELIMITED_PSAID",
      requiredHeaders = Seq("psaId" -> psaId, "loggedInAsPsa" -> "true")
    )
    ()
  }

  def psaNotFound(psaId: String): Unit = {
    stubGetWithHeaders(
      status = 404,
      body = """{"error":"not found"}""",
      requiredHeaders = Seq("psaId" -> psaId, "loggedInAsPsa" -> "true")
    )
    ()
  }

  def psaBadRequest(psaId: String): Unit = {
    stubGetWithHeaders(
      status = 400,
      body = """{"error":"bad request"}""",
      requiredHeaders = Seq("psaId" -> psaId, "loggedInAsPsa" -> "true")
    )
    ()
  }
}
