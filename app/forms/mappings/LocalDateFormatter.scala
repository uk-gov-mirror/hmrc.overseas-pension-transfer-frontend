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

import play.api.data.format.Formatter
import play.api.i18n.Messages
import play.api.data.FormError

import scala.util.Failure
import scala.util.Success
import scala.util.Try

import java.time.LocalDate
import java.time.Month

private[mappings] class LocalDateFormatter(
  invalidCharacter: String,
  invalidKey: String,
  allRequiredKey: String,
  twoRequiredKey: String,
  requiredKey: String,
  realDateKey: String,
  args: Seq[String] = Seq.empty
)(implicit messages: Messages)
    extends Formatter[LocalDate]
    with Formatters {

  private val fieldKeys: List[String] = List("day", "month", "year")

  private val monthFormatter = new MonthFormatter(
    invalidKey,
    invalidCharacter,
    realDateKey,
    args
  )

  private def validMaxDay(isLeapYear: Boolean) = Map(
    Month.JANUARY   -> 31,
    Month.FEBRUARY  -> (if (isLeapYear) 29 else 28),
    Month.MARCH     -> 31,
    Month.APRIL     -> 30,
    Month.MAY       -> 31,
    Month.JUNE      -> 30,
    Month.JULY      -> 31,
    Month.AUGUST    -> 31,
    Month.SEPTEMBER -> 30,
    Month.OCTOBER   -> 31,
    Month.NOVEMBER  -> 30,
    Month.DECEMBER  -> 31
  )

  private def toDate(key: String, data: Map[String, String]): Either[Seq[FormError], LocalDate] =
    Try(
      Tuple3(data(s"$key.year").toIntOption, monthFormatter.bind(s"$key.month", data), data(s"$key.day").toIntOption)
    ) match {
      case Success((Some(year), Right(month), Some(day))) => handleSuccess(key, year, month, day)
      case Success((year, month, day))                    => handlePartErrors(key, day, month, year)
      case _                                              => Left(Seq(FormError(key, invalidKey, args)))
    }

  private def handleSuccess(key: String, year: Int, month: Int, day: Int) =
    Try(LocalDate.of(year, month, day)) match {
      case Success(date) =>
        Right(date)
      case Failure(_)    =>
        val isLeapYear: Boolean = Seq(
          year % 4 == 0,
          year % 100 == 0 && year % 400 == 0
        ).forall(check => check)

        val validMonth       = month >= 1 && month <= 12
        val validDay         = day >= 1 && day <= 31
        val validDayForMonth = if (validMonth) {
          day >= 1 && day <= validMaxDay(isLeapYear)(Month.of(month))
        } else {
          true
        }

        val dayError   =
          if (!validDay || !validDayForMonth) Some(FormError(key, realDateKey, Seq("day") ++ args)) else None
        val monthError = if (!validMonth) Some(FormError(key, realDateKey, Seq("month") ++ args)) else None

        Left(
          Seq(
            dayError,
            monthError
          ).flatten
        )
    }

  private def handlePartErrors(
    key: String,
    day: Option[Int],
    month: Either[Seq[FormError], Int],
    year: Option[Int]
  ): Left[Seq[FormError], Nothing] = {
    val validDay = day.map(mappedDay => mappedDay >= 1 && mappedDay <= 31)
    val dayError = if (validDay.contains(false)) {
      Some(FormError(key, realDateKey, Seq("day") ++ args))
    } else if (day.isEmpty) {
      Some(FormError(key, invalidCharacter, Seq("day") ++ args))
    } else {
      None
    }

    val monthError = month.left.map { formErrors =>
      formErrors.head
    } match {
      case Right(_)    => None
      case Left(error) => Some(error)
    }

    val yearError = if (year.isEmpty) Some(FormError(key, invalidCharacter, Seq("year") ++ args)) else None

    Left(Seq(dayError, monthError, yearError).flatten)
  }

  private def formatDate(key: String, data: Map[String, String]): Either[Seq[FormError], LocalDate] =
    toDate(key, data)

  override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], LocalDate] = {

    val cleanedData: Map[String, String] =
      data.map { case (k, v) => k -> v.replaceAll("\\s+", "") }

    val fields = fieldKeys.map { field =>
      field -> cleanedData.get(s"$key.$field").filter(_.nonEmpty)
    }.toMap

    lazy val missingFields = fields
      .withFilter(_._2.isEmpty)
      .map(_._1)
      .toList
      .map(field => messages(s"date.error.$field"))

    fields.count { case (_, data) => data.isDefined && data.get != "" } match {
      case 3 =>
        formatDate(key, cleanedData).left.map {
          _.map(_.copy(key = key))
        }

      case 2 =>
        Left(List(FormError(key, requiredKey, missingFields ++ args)))
      case 1 =>
        Left(List(FormError(key, twoRequiredKey, missingFields ++ args)))
      case _ =>
        Left(List(FormError(key, allRequiredKey, args)))
    }
  }

  override def unbind(key: String, value: LocalDate): Map[String, String] =
    Map(
      s"$key.day"   -> value.getDayOfMonth.toString,
      s"$key.month" -> value.getMonthValue.toString,
      s"$key.year"  -> value.getYear.toString
    )
}

private class MonthFormatter(
  invalidKey: String,
  invalidCharacter: String,
  realDateKey: String,
  args: Seq[String] = Seq.empty
) extends Formatter[Int]
    with Formatters {

  private val baseFormatter = stringFormatter(invalidKey, args)

  override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Int] = {

    val months = Month.values.toList

    baseFormatter
      .bind(key, data)
      .flatMap { monthString =>
        months
          .find(month =>
            month.getValue.toString == monthString.replaceAll("^0+", "") ||
              month.toString == monthString.toUpperCase ||
              month.toString.take(3) == monthString.toUpperCase
          )
          .map(parsedMonth => Right(parsedMonth.getValue))
          .getOrElse(
            if (monthString.toIntOption.nonEmpty) {
              Left(List(FormError(key, realDateKey, Seq("month") ++ args)))
            } else {
              Left(List(FormError(key, invalidCharacter, Seq("month") ++ args)))
            }
          )
      }
  }

  override def unbind(key: String, value: Int): Map[String, String] =
    Map(key -> value.toString)
}
