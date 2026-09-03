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

package viewmodels

import base.SpecBase
import config.TestAppConfig
import models.{AllTransfersItem, QtStatus}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.i18n.Messages
import play.api.test.Helpers.*
import uk.gov.hmrc.govukfrontend.views.Aliases.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.table.{HeadCell, TableRow}

import java.time.{Instant, ZoneOffset, ZonedDateTime}

class AllTransfersTableViewModelSpec extends AnyFreeSpec with SpecBase with Matchers {

  implicit val messages: Messages = stubMessagesApi().preferred(Seq.empty)
  TestAppConfig.appConfigEncryptionOn()

  private def textOfHead(h: HeadCell): String = h.content match {
    case Text(s) => s
    case other   => fail(s"Expected Text head content, got: $other")
  }

  private def htmlOf(row: TableRow): String =
    row.content match {
      case HtmlContent(h) => h.toString
      case Text(s)        => s
      case other          => fail(s"Unexpected content in TableRow: $other")
    }

  private val dateR = """<p\s+class="govuk-body(?:\s+govuk-!-margin-bottom-0)?">([^<]+)</p>""".r
  private val timeR = """<p\s+class="govuk-body-s(?:\s+govuk-!-margin-bottom-0)?">([^<]+)</p>""".r

  private def extractDate(html: String): String =
    dateR.findFirstMatchIn(html).map(_.group(1)).getOrElse(html)

  private def extractTime(html: String): String =
    timeR.findFirstMatchIn(html).map(_.group(1)).getOrElse(html)

  private def utc(y: Int, m: Int, d: Int, hh: Int, mm: Int): Instant =
    ZonedDateTime.of(y, m, d, hh, mm, 0, 0, ZoneOffset.UTC).toInstant

  "AllTransfersTableViewModel.from" - {

    "renders headers, a member link, submitted status label, reference, and formatted submission date" in {
      val submitted = AllTransfersItem(
        transferId = userAnswersTransferNumber,
        qtVersion = None,
        nino = None,
        memberFirstName = Some("Ada"),
        memberSurname = Some("Lovelace"),
        submissionDate = Some(utc(2025, 9, 24, 10, 15)),
        lastUpdated = None,
        qtStatus = Some(QtStatus.Submitted),
        pstrNumber = None,
        qtDate = None
      )

      val table = AllTransfersTableViewModel.from(Seq(submitted), 1)

      val heads = table.head.value
      heads.map(textOfHead) mustBe Seq(
        "dashboard.allTransfers.head.member",
        "dashboard.allTransfers.head.status",
        "dashboard.allTransfers.head.reference",
        "dashboard.allTransfers.head.updated"
      )

      val row = table.rows.head
      row must have length 4

      all(row.map(_.classes)) must include("govuk-!-padding-bottom-5")

      val memberHtml = htmlOf(row.head)
      memberHtml must include("""<a href=""")
      memberHtml must include("Ada Lovelace")

      memberHtml must include(s"transferId=${userAnswersTransferNumber.value}")
      memberHtml must include("qtStatus=Submitted")
      memberHtml must include("memberName=Ada+Lovelace")
      memberHtml must include("currentPage=1")

      htmlOf(row(1)) mustBe "dashboard.allTransfers.status.submitted"
      htmlOf(row(2)) mustBe userAnswersTransferNumber.value

      val updatedHtml = htmlOf(row(3))
      extractDate(updatedHtml) mustBe "24 September 2025"
      extractTime(updatedHtml) mustBe "10:15am"
    }

    "renders in-progress label and uses lastUpdated (date+time)" in {
      val inProgress = AllTransfersItem(
        transferId = userAnswersTransferNumber,
        qtVersion = None,
        nino = None,
        memberFirstName = Some("  "),
        memberSurname = Some(""),
        submissionDate = None,
        lastUpdated = Some(utc(2025, 1, 5, 17, 3)),
        qtStatus = Some(QtStatus.InProgress),
        pstrNumber = None,
        qtDate = None
      )

      val table = AllTransfersTableViewModel.from(Seq(inProgress), 2)
      val row   = table.rows.head

      val memberHtml = htmlOf(row.head)
      memberHtml must include(">-</a>")
      memberHtml must include("currentPage=2")
      htmlOf(row(1)) mustBe "dashboard.allTransfers.status.inProgress"
      htmlOf(row(2)) mustBe "dashboard.allTransfers.reference.inProgressText"

      val updatedHtml = htmlOf(row(3))
      extractDate(updatedHtml) mustBe "5 January 2025"
      extractTime(updatedHtml) mustBe "5:03pm"
    }

    "maps Compiled status to submitted label (same as Submitted)" in {
      val compiled = AllTransfersItem(
        transferId = userAnswersTransferNumber,
        qtVersion = None,
        nino = None,
        memberFirstName = Some("Jean"),
        memberSurname = Some("Jarvis"),
        submissionDate = Some(utc(2024, 12, 31, 0, 0)),
        lastUpdated = None,
        qtStatus = Some(QtStatus.Compiled),
        pstrNumber = None,
        qtDate = None
      )

      val table = AllTransfersTableViewModel.from(Seq(compiled), 3)
      val row   = table.rows.head

      htmlOf(row(1)) mustBe "dashboard.allTransfers.status.submitted"

      val updatedHtml = htmlOf(row(3))
      extractDate(updatedHtml) mustBe "31 December 2024"
      extractTime(updatedHtml) mustBe "12:00am"
    }
  }
}
