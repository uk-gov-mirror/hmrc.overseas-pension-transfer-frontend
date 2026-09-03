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

package base

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, OptionValues}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Application, Environment, Mode}
import uk.gov.hmrc.http.HeaderCarrier
import utils.WireMockHelper

import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Awaitable}
import scala.util.Random

trait BaseISpec
    extends AnyWordSpecLike
    with WireMockHelper
    with Matchers
    with OptionValues
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with GuiceOneServerPerSuite {

  private val clockMillis: Long = 1718118467838L
  val clock: Clock              = Clock.fixed(Instant.ofEpochMilli(clockMillis), ZoneId.of("UTC"))

  val now: Instant = Instant.now(clock)

  def servicesConfig: Map[String, String] = Map(
    "play.filters.csrf.header.bypassHeaders.Csrf-Token"            -> "nocheck",
    "microservice.services.overseas-pension-transfer-backend.host" -> WireMockHelper.wireMockHost,
    "microservice.services.overseas-pension-transfer-backend.port" -> WireMockHelper.wireMockPort.toString,
    "microservice.services.address-lookup.host"                    -> WireMockHelper.wireMockHost,
    "microservice.services.address-lookup.port"                    -> WireMockHelper.wireMockPort.toString,
    "microservice.services.pensions-scheme.host"                   -> WireMockHelper.wireMockHost,
    "microservice.services.pensions-scheme.port"                   -> WireMockHelper.wireMockPort.toString,
    "microservice.services.auth.host"                              -> WireMockHelper.wireMockHost,
    "microservice.services.auth.port"                              -> WireMockHelper.wireMockPort.toString,
    "microservice.services.pension-administrator.host"             -> WireMockHelper.wireMockHost,
    "microservice.services.pension-administrator.port"             -> WireMockHelper.wireMockPort.toString,
    "microservice.services.email.host"                             -> WireMockHelper.wireMockHost,
    "microservice.services.email.port"                             -> WireMockHelper.wireMockPort.toString
  )

  def generateNino(prefix: String = "AA"): String = {
    val num    = Random.nextInt(1000000)
    val suffix = "C"
    f"$prefix$num%06d$suffix"
  }

  implicit override lazy val app: Application = new GuiceApplicationBuilder()
    .in(Environment.simple(mode = Mode.Dev))
    .configure(servicesConfig)
    .build()

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val appRouteContext: String = "/report-transfer-qualifying-recognised-overseas-pension-scheme"

  override def beforeAll(): Unit = {
    super.beforeAll()
    startServer()
  }

  override def afterAll(): Unit = {
    stopServer()
    super.afterAll()
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    resetWireMock()
  }

  override def afterEach(): Unit =
    super.afterEach()

  def await[T](awaitable: Awaitable[T]): T = Await.result(awaitable, Duration.Inf)

}
