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
import org.scalatest.freespec.AnyFreeSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.OK
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import viewmodels.govuk.SummaryListFluency

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import scala.concurrent.Future

class TaskListControllerSpec extends AnyFreeSpec with SpecBase with SummaryListFluency with MockitoSugar {

  "TaskListController" - {

    "must return OK and NOT persist when no task statuses change" in {

      val app =
        applicationBuilder()
          .build()

      running(app) {
        val req = FakeRequest(GET, controllers.routes.TaskListController.onPageLoad().url)
        val res = route(app, req).value

        status(res) mustEqual OK
      }
    }

    "fromDashboard" - {
      "must redirect to TaskList onPageLoad and set new session data" in {
        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

        val app =
          applicationBuilder()
            .build()
        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
        running(app) {
          val req = FakeRequest(GET, controllers.routes.TaskListController.fromDashboard(userAnswersTransferNumber).url)
          val res = route(app, req).value

          status(res) mustBe SEE_OTHER
          redirectLocation(res) mustBe Some(controllers.routes.TaskListController.onPageLoad().url)
        }
      }
    }
  }
}
