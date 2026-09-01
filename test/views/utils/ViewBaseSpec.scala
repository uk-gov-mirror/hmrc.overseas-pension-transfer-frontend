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

package views.utils

import base.SpecBase
import org.apache.pekko.stream.Materializer
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.freespec.AnyFreeSpec
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.mvc.{AnyContentAsEmpty, MessagesControllerComponents}
import play.api.test.FakeRequest
import play.twirl.api.Html
import uk.gov.hmrc.http.HeaderCarrier

import scala.jdk.CollectionConverters.CollectionHasAsScala

trait ViewBaseSpec extends AnyFreeSpec with SpecBase {

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  implicit lazy val messagesApi: MessagesApi = applicationBuilder().injector().instanceOf(classOf[MessagesApi])
  implicit lazy val messages: Messages       = messagesApi.preferred(FakeRequest())

  implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  implicit val lang: Lang        = mock[Lang]
  implicit val mat: Materializer = mock[Materializer]

  val mockMcc: MessagesControllerComponents = mock[MessagesControllerComponents]

  def doc(body: String): Document = Jsoup.parse(body)

  def pageWithBackLink(html: Html): Unit =
    "have a back link" in
      assert(
        doc(html.body).getElementsByClass("govuk-back-link") != null,
        "\n\nElement " + "govuk-back-link" + " was not rendered on the page.\n"
      )

  def pageWithErrors(view: Html, idSelectorPrefix: String, errorMessage: String): Unit =
    s"show errors correctly for $idSelectorPrefix field" in {
      doc(view.body).getElementsByClass("govuk-error-summary__list").first().text() must include(messages(errorMessage))

      doc(view.body).getElementById(s"$idSelectorPrefix-error").text() must include(messages(errorMessage))

    }

  def pageWithTitle(view: Html, message: String): Unit =
    "show correct page title" in {
      doc(view.body)
        .getElementsByTag("title")
        .first()
        .text mustBe s"${messages(message)} - ${messages("service.name")} - GOV.UK"
    }

  def pageWithH1(view: Html, message: String): Unit =
    "show correct h1" in {
      doc(view.body).getElementsByTag("h1").text() mustBe messages(message)
    }

  def pageWithHeadings(view: Html, headerLevel: String, message: String*): Unit =
    "show correct headings" in {
      doc(view.body).getElementById("main-content").getElementsByTag(headerLevel).eachText().toArray mustBe message
        .map(messageKey => messages(messageKey))
        .toArray
    }

  def pageWithLinks(view: Html, links: (String, String)*): Unit =
    "show correct links" in {
      val document = doc(view.body)
      links.foreach { case (text, url) =>
        val allLinks     = document.select("a[href]")
        val matchingLink = allLinks.asScala.find { link =>
          val href = link.attr("href")
          href == url || href.endsWith(url)
        }

        assert(
          matchingLink.isDefined,
          s"Link with href '$url' was not found. Available hrefs: ${allLinks.asScala.map(_.attr("href")).mkString(", ")}"
        )

        val linkText = matchingLink.get.text()
        assert(
          linkText.contains(messages(text)) || linkText.contains(text),
          s"Link text '$linkText' did not contain expected text '${messages(text)}' or '$text'"
        )
      }
    }

  def pageWithText(view: Html, expectedText: String*): Unit =
    s"show correct text: $expectedText" in {
      doc(view.body).getElementById("main-content").getElementsByTag("p").eachText().toArray mustBe
        expectedText.map(messageKey => messages(messageKey)).toArray
    }

  def pageWithRadioButtons(view: Html, expectedText: String*): Unit =
    "show correct radio buttons" in {
      val radioItems = doc(view.body).getElementsByClass("govuk-radios__item")

      radioItems.size() mustBe expectedText.size

      expectedText.zipWithIndex.foreach { case (messageKey, index) =>
        radioItems.get(index).text() must include(messages(messageKey))
      }
    }

  def pageWithBulletList(view: Html, expectedText: String*): Unit =
    "show correct bullet list items" in {
      val bulletPoints = doc(view.body).getElementsByClass("govuk-list--bullet").first().getElementsByTag("li")

      bulletPoints.eachText().toArray() mustBe expectedText.map(messageKey => messages(messageKey)).toArray
    }

  def pageWithSubmitButton(view: Html, buttonText: String): Unit =
    "show correct submit button" in {
      val button = doc(view.body).getElementsByClass("govuk-button").first()

      button.text() mustBe messages(buttonText)
    }

  def pageWithInputField(view: Html, fieldId: String, labelText: String): Unit =
    "show correct input field" in {
      val input = doc(view.body).getElementById(fieldId)
      val label = doc(view.body).getElementsByAttributeValue("for", fieldId).first()

      assert(input != null, s"\n\nInput field with id '$fieldId' was not rendered on the page.\n")
      label.text() must include(messages(labelText))
    }

  def pageWithConfirmationPanel(view: Html, headingText: String, bodyText: String, referenceNumber: String): Unit =
    "show correct confirmation panel" in {
      val panel      = doc(view.body).getElementsByClass("govuk-panel--confirmation").first()
      val panelTitle = panel.getElementsByClass("govuk-panel__title").first()
      val panelBody  = panel.getElementsByClass("govuk-panel__body").first()

      panelTitle.text() mustBe messages(headingText)
      panelBody.text() must include(messages(bodyText))
      panelBody.text() must include(referenceNumber)
    }

  def pageWithRadioButtonsAndHints(view: Html, expectedRadios: (String, String)*): Unit =
    "show correct radio buttons with hints" in {
      val radioItems = doc(view.body).getElementsByClass("govuk-radios__item")

      radioItems.size() mustBe expectedRadios.size

      expectedRadios.zipWithIndex.foreach { case ((labelKey, hintKey), index) =>
        val radioItem = radioItems.get(index)
        val label     = radioItem.getElementsByClass("govuk-radios__label").first()
        val hint      = radioItem.getElementsByClass("govuk-radios__hint").first()

        label.text() mustBe messages(labelKey)
        if (hint != null) {
          hint.text() mustBe messages(hintKey)
        } else {
          assert(true)
        }
      }
    }

  def pageWithMultipleInputFields(view: Html, fields: (String, String)*): Unit =
    "display all required input fields with correct labels" in
      fields.foreach { case (fieldId, labelText) =>
        val field = doc(view.body).getElementById(fieldId)
        assert(field != null, s"\n\nInput field '$fieldId' was not rendered on the page.\n")

        val label = doc(view.body).select(s"label[for=$fieldId]").first()
        assert(label != null, s"\n\nLabel for field '$fieldId' was not found.\n")
        label.text() must include(messages(labelText))
      }

}
