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

package controllers.transferDetails.assetsMiniJourneys.unquotedShares

import base.SpecBase
import controllers.transferDetails.assetsMiniJourneys.AssetsMiniJourneysRoutes
import forms.transferDetails.assetsMiniJourneys.unquotedShares.UnquotedSharesConfirmRemovalFormProvider
import models.NormalMode
import models.assets.UnquotedSharesEntry
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.freespec.AnyFreeSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.assets.UnquotedSharesQuery
import views.html.transferDetails.assetsMiniJourneys.unquotedShares.UnquotedSharesConfirmRemovalView

import scala.concurrent.Future

class UnquotedSharesConfirmRemovalControllerSpec extends AnyFreeSpec with SpecBase with MockitoSugar {

  private val formProvider = new UnquotedSharesConfirmRemovalFormProvider()
  private val form         = formProvider()

  "UnquotedSharesConfirmRemoval Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder().build()

      running(application) {
        val request =
          FakeRequest(GET, AssetsMiniJourneysRoutes.UnquotedSharesConfirmRemovalController.onPageLoad(1).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[UnquotedSharesConfirmRemovalView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, 1)(fakeDisplayRequest(request), messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {
      val entries     = List(UnquotedSharesEntry("Company", 1000, 20, "Preferred"))
      val userAnswers = emptyUserAnswers.set(UnquotedSharesQuery, entries).success.value

      val application = applicationBuilder(userAnswers = userAnswers).build()
      when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

      when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

      running(application) {
        val request =
          FakeRequest(POST, AssetsMiniJourneysRoutes.UnquotedSharesConfirmRemovalController.onPageLoad(0).url)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual AssetsMiniJourneysRoutes.UnquotedSharesAmendContinueController
          .onPageLoad(NormalMode)
          .url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder().build()

      running(application) {
        val request =
          FakeRequest(POST, AssetsMiniJourneysRoutes.UnquotedSharesConfirmRemovalController.onPageLoad(1).url)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[UnquotedSharesConfirmRemovalView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, 1)(
          fakeDisplayRequest(request),
          messages(application)
        ).toString
      }
    }
  }
}
