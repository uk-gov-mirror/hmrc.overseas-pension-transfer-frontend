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

package repositories

import services.EncryptionService
import uk.gov.hmrc.mongo.MongoComponent
import org.mongodb.scala.model.*
import uk.gov.hmrc.mdc.Mdc
import org.mongodb.scala.bson.conversions.Bson
import play.api.libs.json.Format
import models.AllTransfersItem
import models.DashboardData
import models.QtStatus
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import config.FrontendAppConfig
import models.DashboardData.{encryptedFormat, unencryptedFormat}

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import java.time.Clock
import java.time.Instant
import java.time.Period
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DashboardSessionRepository @Inject() (
  mongoComponent: MongoComponent,
  encryptionService: EncryptionService,
  appConfig: FrontendAppConfig,
  clock: Clock
)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[DashboardData](
      collectionName = "dashboard-data",
      mongoComponent = mongoComponent,
      domainFormat = if (appConfig.mongoDBEncryption) { encryptedFormat(encryptionService) }
      else { unencryptedFormat },
      indexes = Seq(
        IndexModel(
          Indexes.ascending("lastUpdated"),
          IndexOptions()
            .name("lastUpdatedIdx")
            .expireAfter(appConfig.dashboardCacheTtl, TimeUnit.SECONDS)
        )
      )
    ) {

  implicit val instantFormat: Format[Instant] = MongoJavatimeFormats.instantFormat

  private val TtlDays          = 25
  private val WarningDays      = 2
  private val WarningThreshold = Period.ofDays(TtlDays - WarningDays)

  def findExpiringWithin2Days(allTransfers: Seq[AllTransfersItem]): Seq[AllTransfersItem] = {
    val now               = Instant.now(clock)
    val warningThreshold  = now.minus(WarningThreshold)
    val expirableStatuses = Seq(QtStatus.InProgress, QtStatus.AmendInProgress)

    allTransfers.filter { t =>
      t.qtStatus.exists(expirableStatuses.contains) &&
      t.lastUpdated.exists(_.isBefore(warningThreshold))
    }
  }

  private def byId(id: String): Bson = Filters.equal("_id", id)

  def keepAlive(id: String): Future[Boolean] = Mdc.preservingMdc {
    collection
      .updateOne(
        filter = byId(id),
        update = Updates.set("lastUpdated", Instant.now(clock))
      )
      .toFuture()
      .map(_ => true)
  }

  def get(id: String): Future[Option[DashboardData]] = Mdc.preservingMdc {
    keepAlive(id).flatMap { _ =>
      collection
        .find(byId(id))
        .headOption()
    }
  }

  def set(data: DashboardData): Future[Boolean] = Mdc.preservingMdc {

    val updatedData = data copy (lastUpdated = Instant.now(clock))

    collection
      .replaceOne(
        filter = byId(updatedData.id),
        replacement = updatedData,
        options = ReplaceOptions().upsert(true)
      )
      .toFuture()
      .map(_ => true)
  }

  def clear(id: String): Future[Boolean] = Mdc.preservingMdc {
    collection
      .deleteOne(byId(id))
      .toFuture()
      .map(_ => true)
  }
}
