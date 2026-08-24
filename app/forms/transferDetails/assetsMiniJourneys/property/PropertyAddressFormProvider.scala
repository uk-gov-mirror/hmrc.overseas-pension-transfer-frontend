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

package forms.transferDetails.assetsMiniJourneys.property

import utils.AppUtils
import forms.mappings.Mappings
import forms.mappings.Regex
import play.api.data.Forms._
import models.address._
import play.api.data._

import javax.inject.Inject

sealed trait PropertyAddressFormDataTrait {
  val addressLine1: String
  val addressLine2: String
  val addressLine3: Option[String]
  val addressLine4: Option[String]
  val addressLine5: Option[String]
  val countryCode: String
  val postcode: Option[String]

  def town: Option[String]        = addressLine3
  def county: Option[String]      = addressLine4
  def poBoxNumber: Option[String] = addressLine5
}

case class PropertyAddressFormData(
  addressLine1: String,
  addressLine2: String,
  addressLine3: Option[String],
  addressLine4: Option[String],
  postcode: Option[String],
  addressLine5: Option[String],
  countryCode: String
) extends PropertyAddressFormDataTrait

object PropertyAddressFormDataTrait {

  def fromDomain(address: PropertyAddress): PropertyAddressFormData =
    PropertyAddressFormData(
      addressLine1 = address.addressLine1,
      addressLine2 = address.addressLine2,
      addressLine3 = address.town,
      addressLine4 = address.county,
      addressLine5 = address.poBoxNumber,
      countryCode = address.country.code,
      postcode = address.postcode
    )
}

class PropertyAddressFormProvider @Inject() extends Mappings with Regex with AppUtils {

  private[property] def addressLineMapping(line: Int): (String, Mapping[String]) =
    s"addressLine$line" -> text(s"propertyAddress.error.addressLine$line.required")
      .transform[String](input => input.trim, identity)
      .verifying(maxLength(s"common.addressInput.error.addressLine$line.length"))
      .verifying(regexp(addressLinesRegex, s"common.addressInput.error.addressLine$line.pattern"))

  private[property] def optionalAddressLineMapping(line: Int): (String, Mapping[Option[String]]) =
    s"addressLine$line" -> optional(
      Forms.text
        .transform[String](input => input.trim, identity)
        .verifying(maxLength(s"common.addressInput.error.addressLine$line.length"))
        .verifying(regexp(addressLinesRegex, s"common.addressInput.error.addressLine$line.pattern"))
    )

  private def countryMapping(): (String, FieldMapping[String]) =
    "countryCode" -> text("common.addressInput.error.countryCode.required")

  private def postcodeMapping(): (String, Mapping[Option[String]]) = "postcode" -> optional(
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
  )

  def apply(): Form[PropertyAddressFormData] =
    Form(
      mapping(
        addressLineMapping(1),
        addressLineMapping(2),
        optionalAddressLineMapping(3),
        optionalAddressLineMapping(4),
        postcodeMapping(),
        optionalAddressLineMapping(5),
        countryMapping()
      )(PropertyAddressFormData.apply)(data =>
        Some(
          (
            data.addressLine1,
            data.addressLine2,
            data.addressLine3,
            data.addressLine4,
            data.postcode,
            data.addressLine5,
            data.countryCode
          )
        )
      )
    )
}
