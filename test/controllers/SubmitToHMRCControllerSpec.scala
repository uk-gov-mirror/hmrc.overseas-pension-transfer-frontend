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

package controllers

import base.SpecBase
import controllers.actions.*
import forms.SubmitToHMRCFormProvider
import models.NormalMode
import models.authentication.{PspId, PspUser}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.freespec.AnyFreeSpec
import org.scalatestplus.mockito.MockitoSugar
import pages.SubmitToHMRCPage
import play.api.mvc.AnyContentAsFormUrlEncoded
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import views.html.SubmitToHMRCView

import scala.concurrent.Future

class SubmitToHMRCControllerSpec extends AnyFreeSpec with SpecBase with MockitoSugar {

  private val formProvider = new SubmitToHMRCFormProvider()
  private val form         = formProvider()

  private lazy val submitToHMRCRoute = routes.SubmitToHMRCController.onPageLoad(NormalMode).url

  "SubmitToHMRC Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder().build()

      running(application) {
        val request = FakeRequest(GET, submitToHMRCRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[SubmitToHMRCView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode)(
          fakeDisplayRequest(request),
          messages(application)
        ).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = emptyUserAnswers.set(SubmitToHMRCPage, true).success.value

      val application = applicationBuilder(userAnswers = userAnswers).build()

      running(application) {
        val request = FakeRequest(GET, submitToHMRCRoute)

        val view = application.injector.instanceOf[SubmitToHMRCView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(true), NormalMode)(
          fakeDisplayRequest(request),
          messages(application)
        ).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = emptyUserAnswers)
          .build()

      running(application) {
        val request =
          FakeRequest(POST, submitToHMRCRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        val ua = emptyUserAnswers.set(SubmitToHMRCPage, true).success.value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual SubmitToHMRCPage.nextPageWith(NormalMode, ua, psaUser).url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder().build()

      running(application) {
        val request =
          FakeRequest(POST, submitToHMRCRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[SubmitToHMRCView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode)(
          fakeDisplayRequest(request),
          messages(application)
        ).toString
      }
    }

    "must redirect to PSA declaration screen for PSA when value is true" in {

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application = applicationBuilder().build()

      running(application) {
        val request: FakeRequest[AnyContentAsFormUrlEncoded] =
          FakeRequest(POST, submitToHMRCRoute).withFormUrlEncodedBody("value" -> "true")

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        val ua = emptyUserAnswers.set(SubmitToHMRCPage, true).success.value
        redirectLocation(result).value mustEqual SubmitToHMRCPage.nextPageWith(NormalMode, ua, psaUser).url
      }
    }

    "must redirect to PSP declaration screen for PSP when value is true" in {

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val pspUser = PspUser(PspId("A123456"), "userInternalId", affinityGroup = Individual)

      val cc = stubControllerComponents()

      val fakeIdentifierAction =
        new FakeIdentifierActionWithUserType(pspUser, cc.parsers.defaultBodyParser)(cc.executionContext)

      val application =
        applicationBuilder(emptyUserAnswers, sessionDataMemberNameQtNumber, Some(fakeIdentifierAction)).build()

      running(application) {
        val request: FakeRequest[AnyContentAsFormUrlEncoded] =
          FakeRequest(POST, submitToHMRCRoute).withFormUrlEncodedBody("value" -> "true")

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        val ua = emptyUserAnswers.set(SubmitToHMRCPage, true).success.value

        redirectLocation(result).value mustEqual SubmitToHMRCPage.nextPageWith(NormalMode, ua, pspUser).url
      }
    }

    "must redirect to task list when value is false, regardless of user type" in {

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val pspUser = PspUser(PspId("A123456"), "userInternalId", affinityGroup = Individual)

      val cc = stubControllerComponents()

      val fakeIdentifierAction =
        new FakeIdentifierActionWithUserType(pspUser, cc.parsers.defaultBodyParser)(cc.executionContext)

      val application =
        applicationBuilder(emptyUserAnswers, sessionDataMemberNameQtNumber, Some(fakeIdentifierAction)).build()

      running(application) {
        val request: FakeRequest[AnyContentAsFormUrlEncoded] =
          FakeRequest(POST, submitToHMRCRoute).withFormUrlEncodedBody("value" -> "false")

        val result = route(application, request).value

        val ua = emptyUserAnswers.set(SubmitToHMRCPage, false).success.value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual SubmitToHMRCPage.nextPageWith(NormalMode, ua, pspUser).url
      }
    }
  }
}
