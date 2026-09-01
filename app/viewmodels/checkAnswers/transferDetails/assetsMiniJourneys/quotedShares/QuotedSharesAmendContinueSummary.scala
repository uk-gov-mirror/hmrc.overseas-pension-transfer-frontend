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

package viewmodels.checkAnswers.transferDetails.assetsMiniJourneys.quotedShares

import utils.AppUtils
import viewmodels.implicits._
import handlers.AssetThresholdHandler
import uk.gov.hmrc.govukfrontend.views.Aliases.HtmlContent
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.Key
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import pages.transferDetails.assetsMiniJourneys.quotedShares.MoreQuotedSharesDeclarationPage
import models.assets.TypeOfAsset
import controllers.transferDetails.assetsMiniJourneys.AssetsMiniJourneysRoutes
import models.Mode
import models.UserAnswers
import viewmodels.govuk.summarylist._
import queries.assets.QuotedSharesQuery
import play.api.i18n.Messages
import uk.gov.hmrc.hmrcfrontend.views.viewmodels.addtoalist.ListItem

object QuotedSharesAmendContinueSummary extends AppUtils {
  private val threshold = 5

  def row(mode: Mode, userAnswers: UserAnswers, showChangeLink: Boolean = true)(implicit
    messages: Messages
  ): Option[SummaryListRow] = {
    val maybeEntries = userAnswers.get(QuotedSharesQuery)
    val count        = AssetThresholdHandler.getAssetCount(userAnswers, TypeOfAsset.QuotedShares)
    val valueText    = messages("quotedSharesAmendContinue.summary.value", maybeEntries.map(_.size).getOrElse(0))

    maybeEntries match {
      case Some(entries) if entries.nonEmpty =>
        val changeUrl =
          if (count < threshold) {
            AssetsMiniJourneysRoutes.QuotedSharesAmendContinueController.onPageLoad(mode).url
          } else {
            controllers.transferDetails.assetsMiniJourneys.quotedShares.routes.MoreQuotedSharesDeclarationController
              .onPageLoad(mode)
              .url
          }

        val actions =
          if (showChangeLink) {
            Seq(
              ActionItemViewModel("site.change", changeUrl)
                .withVisuallyHiddenText(messages("quotedSharesAmendContinue.change.hidden"))
            )
          } else {
            Seq.empty
          }

        Some(
          SummaryListRowViewModel(
            key = "quotedSharesAmendContinue.checkYourAnswersLabel",
            value = ValueViewModel(valueText),
            actions = actions
          )
        )
      case _                                 => None
    }
  }

  def moreThanFiveQuotedSharesRow(mode: Mode, userAnswers: UserAnswers, showChangeLinks: Boolean)(implicit
    messages: Messages
  ): Option[SummaryListRow] =
    userAnswers.get(MoreQuotedSharesDeclarationPage).filter(identity).map { _ =>
      SummaryListRowViewModel(
        key = Key(Text(messages("moreThanFive.quotedShares.checkYourAnswersLabel"))),
        value = ValueViewModel(HtmlContent(messages("site.yes"))),
        actions = if (showChangeLinks) {
          Seq(
            ActionItemViewModel(
              content = Text(messages("site.change")),
              href =
                controllers.transferDetails.assetsMiniJourneys.quotedShares.routes.MoreQuotedSharesDeclarationController
                  .onPageLoad(mode)
                  .url
            ).withVisuallyHiddenText(messages("moreThanFive.quotedShares.change.hidden"))
          )
        } else {
          Nil
        }
      )
    }

  def rows(mode: Mode, answers: UserAnswers): Seq[ListItem] = {
    val maybeEntries = answers.get(QuotedSharesQuery)
    maybeEntries.getOrElse(Nil).zipWithIndex.map { case (entry, index) =>
      ListItem(
        name = entry.companyName,
        changeUrl = AssetsMiniJourneysRoutes.QuotedSharesCYAController.onPageLoad(mode, index).url,
        removeUrl = AssetsMiniJourneysRoutes.QuotedSharesConfirmRemovalController.onPageLoad(index).url
      )
    }
  }
}
