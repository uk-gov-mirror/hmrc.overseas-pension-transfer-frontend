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

package forms.transferDetails.assetsMiniJourneys.otherAssets

import forms.mappings.Mappings
import forms.mappings.Regex
import play.api.data.Form

import javax.inject.Inject

class OtherAssetsDescriptionFormProvider @Inject() extends Mappings with Regex {

  private val maxLen = 160

  def apply(): Form[String] =
    Form(
      "value" -> text("assetValueDescription.error.required")
        .transform[String](input => input.trim, identity)
        .verifying(maxLength("assetValueDescription.error.length", maxLen))
        .verifying(regexp(descriptionRegex, "assetValueDescription.error.pattern"))
    )
}
