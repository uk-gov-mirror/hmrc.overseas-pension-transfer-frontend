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

package viewmodels.checkAnswers.transferDetails

import viewmodels.implicits._
import pages.transferDetails.assetsMiniJourneys.cash.CashAmountInTransferPage
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import controllers.transferDetails.assetsMiniJourneys.AssetsMiniJourneysRoutes
import utils.CurrencyFormats.currencyFormat
import models.Mode
import models.UserAnswers
import play.api.i18n.Messages
import viewmodels.govuk.summarylist._

object CashAmountInTransferSummary {

  def row(mode: Mode, answers: UserAnswers, showChangeLink: Boolean = true)(implicit
    messages: Messages
  ): Option[SummaryListRow] =
    answers.get(CashAmountInTransferPage).map { answer =>
      val actions =
        if (showChangeLink) {
          Seq(
            ActionItemViewModel(
              "site.change",
              AssetsMiniJourneysRoutes.CashAmountInTransferController.onPageLoad(mode).url
            ).withVisuallyHiddenText(messages("cashAmountInTransfer.change.hidden"))
          )
        } else {
          Seq.empty
        }

      SummaryListRowViewModel(
        key = "cashAmountInTransfer.checkYourAnswersLabel",
        value = ValueViewModel(currencyFormat(answer)),
        actions = actions
      )
    }

}
