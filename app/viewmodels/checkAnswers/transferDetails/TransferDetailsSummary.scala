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

import pages.transferDetails.IsTransferCashOnlyPage
import viewmodels.checkAnswers.transferDetails.assetsMiniJourneys.property.PropertyAmendContinueSummary
import viewmodels.checkAnswers.transferDetails.assetsMiniJourneys.quotedShares.QuotedSharesAmendContinueSummary
import viewmodels.checkAnswers.transferDetails.assetsMiniJourneys.otherAssets.OtherAssetsAmendContinueSummary
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.checkAnswers.transferDetails.assetsMiniJourneys.unquotedShares.UnquotedSharesAmendContinueSummary
import models.Mode
import models.UserAnswers
import play.api.i18n.Messages

case object TransferDetailsSummary {

  def rows(mode: Mode, userAnswers: UserAnswers, showChangeLinks: Boolean = true)(implicit
    messages: Messages
  ): Seq[SummaryListRow] = {
    val showCashAmount       = userAnswers.get(IsTransferCashOnlyPage).contains(false)
    val cashAmountInTransfer =
      if (showCashAmount) CashAmountInTransferSummary.row(mode, userAnswers, showChangeLinks) else None

    Seq(
      OverseasTransferAllowanceSummary.row(mode, userAnswers, showChangeLinks),
      AmountOfTransferSummary.row(mode, userAnswers, showChangeLinks),
      IsTransferTaxableSummary.row(mode, userAnswers, showChangeLinks),
      WhyTransferIsTaxableSummary.row(mode, userAnswers, showChangeLinks),
      WhyTransferIsNotTaxableSummary.row(mode, userAnswers, showChangeLinks),
      ApplicableTaxExclusionsSummary.row(mode, userAnswers, showChangeLinks),
      AmountOfTaxDeductedSummary.row(mode, userAnswers, showChangeLinks),
      NetTransferAmountSummary.row(mode, userAnswers, showChangeLinks),
      DateOfTransferSummary.row(mode, userAnswers, showChangeLinks),
      IsTransferCashOnlySummary.row(mode, userAnswers, showChangeLinks),
      TypeOfAssetSummary.row(mode, userAnswers, showChangeLinks),
      cashAmountInTransfer,
      UnquotedSharesAmendContinueSummary.row(mode, userAnswers, showChangeLinks),
      UnquotedSharesAmendContinueSummary.moreThanFiveUnquotedSharesRow(mode, userAnswers, showChangeLinks),
      QuotedSharesAmendContinueSummary.row(mode, userAnswers, showChangeLinks),
      QuotedSharesAmendContinueSummary.moreThanFiveQuotedSharesRow(mode, userAnswers, showChangeLinks),
      PropertyAmendContinueSummary.row(mode, userAnswers, showChangeLinks),
      PropertyAmendContinueSummary.moreThanFivePropertiesRow(mode, userAnswers, showChangeLinks),
      OtherAssetsAmendContinueSummary.row(mode, userAnswers, showChangeLinks),
      OtherAssetsAmendContinueSummary.moreThanFiveOtherAssetsRow(mode, userAnswers, showChangeLinks)
    ).flatten
  }
}
