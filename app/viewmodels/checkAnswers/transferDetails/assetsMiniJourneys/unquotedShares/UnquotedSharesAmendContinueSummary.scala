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

package viewmodels.checkAnswers.transferDetails.assetsMiniJourneys.unquotedShares

import utils.AppUtils
import viewmodels.implicits._
import handlers.AssetThresholdHandler
import uk.gov.hmrc.govukfrontend.views.Aliases.HtmlContent
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.Key
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import models.assets.TypeOfAsset
import controllers.transferDetails.assetsMiniJourneys.AssetsMiniJourneysRoutes
import models.Mode
import models.UserAnswers
import viewmodels.govuk.summarylist._
import queries.assets.UnquotedSharesQuery
import pages.transferDetails.assetsMiniJourneys.unquotedShares.MoreUnquotedSharesDeclarationPage
import play.api.i18n.Messages
import uk.gov.hmrc.hmrcfrontend.views.viewmodels.addtoalist.ListItem

object UnquotedSharesAmendContinueSummary extends AppUtils {

  private val threshold = 5

  def row(mode: Mode, userAnswers: UserAnswers, showChangeLink: Boolean = true)(implicit
    messages: Messages
  ): Option[SummaryListRow] = {
    val maybeEntries = userAnswers.get(UnquotedSharesQuery)
    val count        = AssetThresholdHandler.getAssetCount(userAnswers, TypeOfAsset.UnquotedShares)
    val valueText    = messages("unquotedSharesAmendContinue.summary.value", maybeEntries.map(_.size).getOrElse(0))

    maybeEntries match {
      case Some(entries) if entries.nonEmpty =>
        val changeUrl =
          if (count < threshold) {
            AssetsMiniJourneysRoutes.UnquotedSharesAmendContinueController.onPageLoad(mode).url
          } else {
            controllers.transferDetails.assetsMiniJourneys.unquotedShares.routes.MoreUnquotedSharesDeclarationController
              .onPageLoad(mode)
              .url
          }

        val actions =
          if (showChangeLink) {
            Seq(
              ActionItemViewModel("site.change", changeUrl)
                .withVisuallyHiddenText(messages("unquotedSharesAmendContinue.change.hidden"))
            )
          } else {
            Seq.empty
          }

        Some(
          SummaryListRowViewModel(
            key = "unquotedSharesAmendContinue.checkYourAnswersLabel",
            value = ValueViewModel(valueText),
            actions = actions
          )
        )
      case _                                 => None
    }
  }

  def moreThanFiveUnquotedSharesRow(mode: Mode, userAnswers: UserAnswers, showChangeLinks: Boolean)(implicit
    messages: Messages
  ): Option[SummaryListRow] =
    userAnswers.get(MoreUnquotedSharesDeclarationPage).filter(identity).map { _ =>
      SummaryListRowViewModel(
        key = Key(Text(messages("moreThanFive.unquotedShares.checkYourAnswersLabel"))),
        value = ValueViewModel(HtmlContent(messages("site.yes"))),
        actions = if (showChangeLinks) {
          Seq(
            ActionItemViewModel(
              content = Text(messages("site.change")),
              href =
                controllers.transferDetails.assetsMiniJourneys.unquotedShares.routes.MoreUnquotedSharesDeclarationController
                  .onPageLoad(mode)
                  .url
            ).withVisuallyHiddenText(messages("moreThanFive.unquotedShares.change.hidden"))
          )
        } else {
          Nil
        }
      )
    }

  def rows(mode: Mode, answers: UserAnswers): Seq[ListItem] = {
    val maybeEntries = answers.get(UnquotedSharesQuery)
    maybeEntries.getOrElse(Nil).zipWithIndex.map { case (entry, index) =>
      ListItem(
        name = entry.companyName,
        changeUrl = AssetsMiniJourneysRoutes.UnquotedSharesCYAController.onPageLoad(mode, index).url,
        removeUrl = AssetsMiniJourneysRoutes.UnquotedSharesConfirmRemovalController.onPageLoad(index).url
      )
    }
  }
}
