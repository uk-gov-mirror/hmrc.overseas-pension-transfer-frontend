/*
 * Copyright 2024 HM Revenue & Customs
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

package forms.mappings

import generators.Generators
import org.scalacheck.Gen
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.data.validation.{Invalid, Valid}
import utils.CurrencyFormats

import java.time.LocalDate

class ConstraintsSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks with Generators with Constraints {
  val lineLength = 10
  "firstError" - {

    "must return Valid when all constraints pass" in {
      val result = firstError(maxLength("error.length", lineLength), regexp("""^\w+$""", "error.regexp"))("foo")
      result mustEqual Valid
    }

    "must return Invalid when the first constraint fails" in {
      val result = firstError(maxLength("error.length", lineLength), regexp("""^\w+$""", "error.regexp"))("a" * 36)
      result mustEqual Invalid("error.length", lineLength)
    }

    "must return Invalid when the second constraint fails" in {
      val result = firstError(maxLength("error.length", lineLength), regexp("""^\w+$""", "error.regexp"))("")
      result mustEqual Invalid("error.regexp", """^\w+$""")
    }

    "must return Invalid for the first error when both constraints fail" in {
      val result = firstError(maxLength("error.length", -1), regexp("""^\w+$""", "error.regexp"))("")
      result mustEqual Invalid("error.length", -1)
    }
  }

  "minimumValue" - {

    "must return Valid for a number greater than the threshold" in {
      val result = minimumValue(1, "error.min").apply(2)
      result mustEqual Valid
    }

    "must return Valid for a number equal to the threshold" in {
      val result = minimumValue(1, "error.min").apply(1)
      result mustEqual Valid
    }

    "must return Invalid for a number below the threshold" in {
      val result = minimumValue(1, "error.min").apply(0)
      result mustEqual Invalid("error.min", 1)
    }
  }

  "maximumValue" - {

    "must return Valid for a number less than the threshold" in {
      val result = maximumValue(1, "error.max").apply(0)
      result mustEqual Valid
    }

    "must return Valid for a number equal to the threshold" in {
      val result = maximumValue(1, "error.max").apply(1)
      result mustEqual Valid
    }

    "must return Invalid for a number above the threshold" in {
      val result = maximumValue(1, "error.max").apply(2)
      result mustEqual Invalid("error.max", 1)
    }
  }

  "regexp" - {

    "must return Valid for an input that matches the expression" in {
      val result = regexp("""^\w+$""", "error.invalid")("foo")
      result mustEqual Valid
    }

    "must return Invalid for an input that does not match the expression" in {
      val result = regexp("""^\d+$""", "error.invalid")("foo")
      result mustEqual Invalid("error.invalid", """^\d+$""")
    }
  }

  "maxLength" - {

    "must return Valid for a string shorter than the allowed length" in {
      val result = maxLength("error.length", lineLength)("a" * 9)
      result mustEqual Valid
    }

    "must return Valid for an empty string" in {
      val result = maxLength("error.length", lineLength)("")
      result mustEqual Valid
    }

    "must return Valid for a string equal to the allowed length" in {
      val result = maxLength("error.length", lineLength)("a" * 10)
      result mustEqual Valid
    }

    "must return Invalid for a string longer than the allowed length" in {
      val result = maxLength("error.length", lineLength)("a" * 11)
      result mustEqual Invalid("error.length", lineLength)
    }
  }
  val startDate: LocalDate = LocalDate.of(2000, 1, 1)
  val endDate: LocalDate   = LocalDate.of(3000, 1, 1)
  "maxDate" - {

    "must return Valid for a date before or equal to the maximum" in {

      val gen: Gen[(LocalDate, LocalDate)] = for {
        max  <- datesBetween(startDate, endDate)
        date <- datesBetween(startDate, max)
      } yield (max, date)

      forAll(gen) { case (max, date) =>
        val result = maxDate(max, "error.future")(date)
        result mustEqual Valid
      }
    }

    "must return Invalid for a date after the maximum" in {

      val gen: Gen[(LocalDate, LocalDate)] = for {
        max  <- datesBetween(startDate, endDate)
        date <- datesBetween(max.plusDays(1), endDate)
      } yield (max, date)

      forAll(gen) { case (max, date) =>
        val result = maxDate(max, "error.future", "foo")(date)
        result mustEqual Invalid("error.future", "foo")
      }
    }
  }

  "minDate" - {

    "must return Valid for a date after or equal to the minimum" in {

      val gen: Gen[(LocalDate, LocalDate)] = for {
        min  <- datesBetween(startDate, endDate)
        date <- datesBetween(min, endDate)
      } yield (min, date)

      forAll(gen) { case (min, date) =>
        val result = minDate(min, "error.past", "foo")(date)
        result mustEqual Valid
      }
    }

    "must return Invalid for a date before the minimum" in {

      val gen: Gen[(LocalDate, LocalDate)] = for {
        min  <- datesBetween(startDate, endDate)
        date <- datesBetween(startDate, min.minusDays(1))
      } yield (min, date)

      forAll(gen) { case (min, date) =>
        val result = minDate(min, "error.past", "foo")(date)
        result mustEqual Invalid("error.past", "foo")
      }
    }
  }

  "minimumCurrency" - {

    "must return Valid for a number greater than the threshold" in {
      val result = minimumCurrency(1, "error.min").apply(BigDecimal(1.01))
      result mustEqual Valid
    }

    "must return Valid for a number equal to the threshold" in {
      val result = minimumCurrency(1, "error.min").apply(1)
      result mustEqual Valid
    }

    "must return Invalid for a number below the threshold" in {
      val result = minimumCurrency(1, "error.min").apply(0.99)
      result mustEqual Invalid("error.min", CurrencyFormats.currencyFormat(1))
    }
  }

  "maximumCurrency" - {

    "must return Valid for a number less than the threshold" in {
      val result = maximumCurrency(1, "error.max").apply(0)
      result mustEqual Valid
    }

    "must return Valid for a number equal to the threshold" in {
      val result = maximumCurrency(1, "error.max").apply(1)
      result mustEqual Valid
    }

    "must return Invalid for a number above the threshold" in {
      val result = maximumCurrency(1, "error.max").apply(1.01)
      result mustEqual Invalid("error.max", CurrencyFormats.currencyFormat(1))
    }
  }
}
