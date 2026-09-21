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

package pages

import play.api.mvc.Call
import models._
import scala.annotation.unused

/*
  Mix this into a Page when you need extra args to help with navigation.

  It is used by the following Page instances:-
   DiscardTransferConfirmPage: to pass version no (String) into the page.
   SubmitToHMRCPage: to pass AuthenticatedUser into the page.
   MiniJourneyNextPageWith: to pass SessionData into the page.

  MiniJourneyNextPageWith is used by various pages in package pages.transferDetails.assetsMiniJourneys
 */
trait NextPageWith[C] { self: Page =>

  protected def nextPageWith(answers: UserAnswers, @unused context: C): Call =
    nextPageNormalMode(answers)

  protected def nextPageCheckModeWith(answers: UserAnswers, @unused context: C): Call =
    nextPageCheckMode(answers)

  protected def nextPageFinalCheckModeWith(answers: UserAnswers, @unused context: C): Call =
    nextPageAmendCheckMode(answers)

  protected def nextPageAmendCheckModeWith(answers: UserAnswers, @unused context: C): Call =
    nextPageAmendCheckMode(answers)

  final def nextPageWith(mode: Mode, answers: UserAnswers, context: C): Call =
    mode match {
      case NormalMode     => nextPageWith(answers, context)
      case CheckMode      => nextPageCheckModeWith(answers, context)
      case FinalCheckMode => nextPageFinalCheckModeWith(answers, context)
      case AmendCheckMode => nextPageAmendCheckModeWith(answers, context)
    }
}
