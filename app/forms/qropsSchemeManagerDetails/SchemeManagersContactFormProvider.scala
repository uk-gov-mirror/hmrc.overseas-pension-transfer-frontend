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

package forms.qropsSchemeManagerDetails

import forms.mappings.Mappings
import forms.mappings.Regex
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import play.api.data.Form

import javax.inject.Inject

class SchemeManagersContactFormProvider @Inject() extends Mappings with Regex {

  def apply(): Form[String] =
    Form(
      "contactNumber" -> text("schemeManagersContact.error.required")
        .transform[String](_.replaceAll("\\s+", ""), identity)
        .transform[String](s => if (s.startsWith("00")) "+" + s.substring(2) else s, identity)
        .transform[String](_.replaceAll("\\D+$", ""), identity)
        .verifying(maxLength("schemeManagersContact.error.length"))
        .verifying("schemeManagersContact.error.pattern", _.matches("^(?!\\+\\+)[\\d()+\\-]+$"))
        .verifying("schemeManagersContact.error.pattern", number => isValidPhoneNumber(number))
    )

  /** Accepts any valid phone number in the world. If it starts with '+', it's treated as an international number. If it
    * doesn't, we fall back to GB as a default region for parsing. See:
    * https://design-system.service.gov.uk/patterns/phone-numbers/
    */
  private def isValidPhoneNumber(raw: String): Boolean = {
    val phoneUtil = PhoneNumberUtil.getInstance()
    try
      if (raw.isEmpty) {
        false
      } else {
        val defaultRegion = if (raw.startsWith("+")) "ZZ" else "GB"
        val parsed        = phoneUtil.parse(raw, defaultRegion)
        phoneUtil.isPossibleNumber(parsed)
      }
    catch {
      case _: NumberParseException => false
    }
  }
}
