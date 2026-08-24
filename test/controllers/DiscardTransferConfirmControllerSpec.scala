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
import forms.DiscardTransferConfirmFormProvider
import models.QtStatus.AmendInProgress
import models.responses.UserAnswersErrorResponse
import models.{AmendCheckMode, NormalMode, UserAnswers}
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.freespec.AnyFreeSpec
import org.scalatestplus.mockito.MockitoSugar
import pages.DiscardTransferConfirmPage
import play.api.inject.bind
import play.api.libs.json.{JsObject, JsString}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.{LockService, UserAnswersService}
import views.html.DiscardTransferConfirmView

import scala.concurrent.Future

class DiscardTransferConfirmControllerSpec extends AnyFreeSpec with SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val formProvider = new DiscardTransferConfirmFormProvider()
  private val form         = formProvider()

  lazy val viewRedirect: UserAnswers => String = _ => controllers.routes.TaskListController.onPageLoad().url

  lazy val amendRedirect: UserAnswers => String = userAnswers =>
    controllers.viewandamend.routes.ViewAmendSubmittedController
      .fromDraft(
        userAnswers.id,
        userAnswers.pstr,
        AmendInProgress,
        "002"
      )
      .url
  override protected def beforeEach(): Unit     = {
    super.beforeEach()
    reset(mockSessionRepository)
  }

  "DiscardTransferConfirm Controller" - {

    Seq(
      NormalMode     -> viewRedirect,
      AmendCheckMode -> amendRedirect
    ).foreach { case (mode, redirectUrl) =>
      lazy val discardTransferConfirmRoute = routes.DiscardTransferConfirmController.onPageLoad(mode).url

      s"in $mode mode" - {
        "must return OK and the correct view for a GET" in {

          val application = applicationBuilder(userAnswers = emptyUserAnswers).build()

          running(application) {
            val request = FakeRequest(GET, discardTransferConfirmRoute)

            val result = route(application, request).value

            val view = application.injector.instanceOf[DiscardTransferConfirmView]

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(form, mode)(request, messages(application)).toString
          }
        }

        "must populate the view correctly on a GET when the question has previously been answered" in {

          val userAnswers = emptyUserAnswers.set(DiscardTransferConfirmPage, true).success.value

          val application = applicationBuilder(userAnswers = userAnswers).build()

          running(application) {
            val request = FakeRequest(GET, discardTransferConfirmRoute)

            val view = application.injector.instanceOf[DiscardTransferConfirmView]

            val result = route(application, request).value

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(form.fill(true), mode)(request, messages(application)).toString
          }
        }

        "must release lock and redirect + clear answers when YES selected" in {
          val userAnswers = emptyUserAnswers.set(DiscardTransferConfirmPage, true).success.value

          val mockUserAnswersService = mock[UserAnswersService]
          val mockLockService        = mock[LockService]

          when(mockLockService.isLocked(any(), any())) thenReturn Future.successful(true)
          when(mockLockService.releaseLock(any(), any())) thenReturn Future.successful(())
          when(mockSessionRepository.clear(any())) thenReturn Future.successful(true)
          when(mockUserAnswersService.clearUserAnswers(any(), any())(any())) thenReturn Future.successful(Right(Done))

          val application =
            applicationBuilder(userAnswers = userAnswers)
              .overrides(
                bind[UserAnswersService].toInstance(mockUserAnswersService),
                bind[LockService].toInstance(mockLockService)
              )
              .build()

          running(application) {
            val request =
              FakeRequest(POST, discardTransferConfirmRoute)
                .withFormUrlEncodedBody(("discardTransfer", "true"))

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.routes.DashboardController.onPageLoad().url

            verify(mockLockService, times(1)).isLocked(any(), any())
            verify(mockLockService, times(1)).releaseLock(any(), any())
            verify(mockSessionRepository, times(1)).clear(any())
            verify(mockUserAnswersService, times(1)).clearUserAnswers(any(), any())(any())
          }
        }

        "must release lock and redirect to correctly when NO selected" in {
          val userAnswers = emptyUserAnswers.set(DiscardTransferConfirmPage, false).success.value

          val mockLockService = mock[LockService]

          when(mockLockService.isLocked(any(), any())) thenReturn Future.successful(true)
          when(mockLockService.releaseLock(any(), any())) thenReturn Future.successful(())

          val sessionDataWithVersion =
            emptySessionData.copy(data = emptySessionData.data ++ JsObject(Map("versionNumber" -> JsString("002"))))

          val application =
            applicationBuilder(userAnswers = userAnswers, sessionData = sessionDataWithVersion)
              .overrides(bind[LockService].toInstance(mockLockService))
              .build()

          running(application) {
            val request =
              FakeRequest(POST, discardTransferConfirmRoute)
                .withFormUrlEncodedBody(("discardTransfer", "false"))

            val result = route(application, request).value

            status(result) mustBe SEE_OTHER
            redirectLocation(result).value mustBe redirectUrl(userAnswers)

            verify(mockLockService, times(1)).isLocked(any(), any())
            verify(mockLockService, times(1)).releaseLock(any(), any())
          }
        }

        "must return Internal Server Error when clearUserAnswers returns a Left(DeleteFailed)" in {
          val userAnswers = emptyUserAnswers.set(DiscardTransferConfirmPage, true).success.value

          val mockUserAnswersService = mock[UserAnswersService]
          val mockLockService        = mock[LockService]

          when(mockLockService.isLocked(any(), any())) thenReturn Future.successful(true)
          when(mockLockService.releaseLock(any(), any())) thenReturn Future.successful(())
          when(mockSessionRepository.clear(any())) thenReturn Future.successful(true)
          when(mockUserAnswersService.clearUserAnswers(any(), any())(any())) thenReturn
            Future.successful(Left(UserAnswersErrorResponse("Error", None)))

          val application =
            applicationBuilder(userAnswers = userAnswers)
              .overrides(
                bind[UserAnswersService].toInstance(mockUserAnswersService),
                bind[LockService].toInstance(mockLockService)
              )
              .build()

          running(application) {
            val request =
              FakeRequest(POST, discardTransferConfirmRoute)
                .withFormUrlEncodedBody(("discardTransfer", "true"))

            val result = route(application, request).value

            status(result) mustEqual INTERNAL_SERVER_ERROR
          }
        }

        "must return a Bad Request and errors when invalid data is submitted" in {

          val application = applicationBuilder(userAnswers = emptyUserAnswers).build()

          running(application) {
            val request =
              FakeRequest(POST, discardTransferConfirmRoute)
                .withFormUrlEncodedBody(("discardTransfer", ""))

            val boundForm = form.bind(Map("discardTransfer" -> ""))

            val view = application.injector.instanceOf[DiscardTransferConfirmView]

            val result = route(application, request).value

            status(result) mustEqual BAD_REQUEST
            contentAsString(result) mustEqual view(boundForm, mode)(request, messages(application)).toString
          }
        }
      }

    }
  }
}
