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

package controllers.auth

import base.SpecBase
import config.FrontendAppConfig
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.freespec.AnyFreeSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.UserAnswersService
import org.mockito.Mockito.reset
import java.net.URLEncoder
import scala.concurrent.Future

class AuthControllerSpec extends AnyFreeSpec with SpecBase with MockitoSugar with BeforeAndAfterEach {
  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockSessionRepository)
  }
  "signOut" - {
    "must clear session-data and redirect to sign out, specifying the exit survey as the continue URL" in {

      val mockUserAnswersService = mock[UserAnswersService]

      when(mockSessionRepository.clear(any())) thenReturn Future.successful(true)
      when(mockUserAnswersService.clearEmptyUserAnswers(any())(any())) thenReturn Future.successful(())

      val application =
        applicationBuilder(emptyUserAnswers)
          .overrides(
            bind[UserAnswersService].toInstance(mockUserAnswersService)
          )
          .build()

      running(application) {

        val appConfig           = application.injector.instanceOf[FrontendAppConfig]
        val request             = FakeRequest(GET, routes.AuthController.signOut().url)
        val result              = route(application, request).value
        val encodedContinueUrl  = URLEncoder.encode(appConfig.exitSurveyUrl, "UTF-8")
        val expectedRedirectUrl = s"${appConfig.signOutUrl}?continue=$encodedContinueUrl"

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual expectedRedirectUrl
        verify(mockSessionRepository, times(1)).clear(eqTo("id"))
      }
    }

    "must clear empty-user-answers on sign out" in {

      val mockUserAnswersService = mock[UserAnswersService]

      when(mockSessionRepository.clear(any())).thenReturn(Future.successful(true))
      when(mockUserAnswersService.clearEmptyUserAnswers(any())(any())).thenReturn(Future.successful(()))

      val application =
        applicationBuilder(emptyUserAnswers)
          .overrides(
            bind[UserAnswersService].toInstance(mockUserAnswersService)
          )
          .build()

      running(application) {

        val request = FakeRequest(GET, routes.AuthController.signOut().url)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        verify(mockUserAnswersService, times(1))
          .clearEmptyUserAnswers(eqTo("id"))(any())
        verify(mockSessionRepository, times(1))
          .clear(eqTo("id"))
      }
    }
  }

  "signOutNoSurvey" - {
    "must redirect to sign out, specifying SignedOut as the continue URL" in {

      val application =
        applicationBuilder(emptyUserAnswers)
          .build()

      running(application) {

        val appConfig           = application.injector.instanceOf[FrontendAppConfig]
        val request             = FakeRequest(GET, routes.AuthController.signOutNoSurvey().url)
        val result              = route(application, request).value
        val encodedContinueUrl  = URLEncoder.encode(appConfig.signedOutRedirectUrl, "UTF-8")
        val expectedRedirectUrl = s"${appConfig.signOutUrl}?continue=$encodedContinueUrl"

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual expectedRedirectUrl
      }
    }
  }
}
