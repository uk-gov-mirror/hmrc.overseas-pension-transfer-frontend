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

package forms.memberDetails

import utils.AppUtils
import forms.mappings.Mappings
import forms.mappings.Regex
import play.api.data.Forms.*
import models.address.*
import models.requests.DisplayRequest
import play.api.data.{Form, Forms, Mapping}

import javax.inject.Inject

case class MembersCurrentAddressFormData(
  addressLine1: String,
  addressLine2: String,
  addressLine3: Option[String],
  addressLine4: Option[String],
  countryCode: String,
  postcode: Option[String],
  poBox: Option[String]
)

object MembersCurrentAddressFormData {

  def fromDomain(address: MembersCurrentAddress): MembersCurrentAddressFormData =
    MembersCurrentAddressFormData(
      addressLine1 = address.addressLine1,
      addressLine2 = address.addressLine2,
      addressLine3 = address.addressLine3,
      addressLine4 = address.addressLine4,
      countryCode = address.country.code,
      postcode = address.postcode,
      poBox = address.poBoxNumber
    )

  def unapply(
    addressFormData: MembersCurrentAddressFormData
  ): Option[(String, String, Option[String], Option[String], String, Option[String], Option[String])] =
    Some(
      (
        addressFormData.addressLine1,
        addressFormData.addressLine2,
        addressFormData.addressLine3,
        addressFormData.addressLine4,
        addressFormData.countryCode,
        addressFormData.postcode,
        addressFormData.poBox
      )
    )
}

class MembersCurrentAddressFormProvider @Inject() extends Mappings with Regex with AppUtils {
  private def addressLineMapping(line: Int, memberName: String): (String, Mapping[String]) =
    s"addressLine$line" -> text(s"membersCurrentAddress.error.addressLine$line.required", Seq(memberName))
      .transform[String](input => input.trim, identity)
      .verifying(maxLength(s"common.addressInput.error.addressLine$line.length"))
      .verifying(regexp(addressLinesRegex, s"common.addressInput.error.addressLine$line.pattern"))

  private def optionalAddressLineMapping(line: Int): (String, Mapping[Option[String]]) =
    s"addressLine$line" -> optional(
      Forms.text
        .transform[String](input => input.trim, identity)
        .verifying(maxLength(s"common.addressInput.error.addressLine$line.length"))
        .verifying(regexp(addressLinesRegex, s"common.addressInput.error.addressLine$line.pattern"))
    )
  def apply()(implicit request: DisplayRequest[_]): Form[MembersCurrentAddressFormData] = {
    val memberName = request.memberName
    Form(
      mapping(
        addressLineMapping(1, memberName),
        addressLineMapping(2, memberName),
        optionalAddressLineMapping(3),
        optionalAddressLineMapping(4),
        "countryCode" -> text("common.addressInput.error.countryCode.required"),
        "postcode"    -> optional(
          Forms.text
            .transform[String](
              raw => formatUkPostcode(raw),
              formatted => formatted
            )
            .verifying(maxLength("common.addressInput.error.postcode.length"))
            .verifying(
              "membersLastUKAddress.error.postcode.incorrect",
              { postcode =>
                val parts = postcode.split("\\s+")
                if (parts.length == 2) {
                  val outcode = parts(0)
                  val incode  = parts(1)
                  (outcode.matches(postcodeOutcodeRegex) && incode.matches(postcodeIncodeRegex)) ||
                  postcode == "GIR 0AA"
                } else {
                  false
                }
              }
            )
        ),
        "poBox"       -> optional(
          Forms.text
            .transform[String](input => input.trim, identity)
            .verifying(maxLength("common.addressInput.error.poBox.length"))
            .verifying(regexp(poBoxRegex, "common.addressInput.error.poBox.pattern"))
        )
      )(MembersCurrentAddressFormData.apply)(MembersCurrentAddressFormData.unapply)
    )
  }
}
