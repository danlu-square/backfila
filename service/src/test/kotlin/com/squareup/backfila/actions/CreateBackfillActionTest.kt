package com.squareup.backfila.actions

import com.google.inject.Module
import com.squareup.backfila.api.ConfigureServiceAction
import com.squareup.backfila.dashboard.CreateBackfillAction
import com.squareup.backfila.dashboard.CreateBackfillRequest
import com.squareup.backfila.service.BackfilaDb
import com.squareup.backfila.service.BackfillRunQuery
import com.squareup.backfila.service.BackfillState
import com.squareup.backfila.service.RunInstanceQuery
import com.squareup.protos.backfila.service.ConfigureServiceRequest
import com.squareup.protos.backfila.service.ServiceType
import misk.MiskCaller
import misk.exceptions.BadRequestException
import misk.hibernate.Query
import misk.hibernate.Transacter
import misk.hibernate.newQuery
import misk.inject.keyOf
import misk.scope.ActionScope
import misk.testing.MiskTest
import misk.testing.MiskTestModule
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.net.HttpURLConnection
import javax.inject.Inject
import kotlin.test.assertNotNull

@MiskTest(startService = true)
class CreateBackfillActionTest {
  @Suppress("unused")
  @MiskTestModule
  val module: Module = BackfilaWebActionTestingModule()

  @Inject lateinit var configureServiceAction: ConfigureServiceAction
  @Inject lateinit var createBackfillAction: CreateBackfillAction
  @Inject lateinit var scope: ActionScope
  @Inject lateinit var queryFactory: Query.Factory
  @Inject @BackfilaDb lateinit var transacter: Transacter

  @Test
  fun serviceDoesntExist() {
    scope(MiskCaller(user = "bob")).use {
      assertThatThrownBy {
        createBackfillAction.create("franklin", CreateBackfillRequest("abc"))
      }.isInstanceOf(BadRequestException::class.java)
    }
  }

  @Test
  fun backfillDoesntExist() {
    scope(MiskCaller(service = "franklin")).use {
      configureServiceAction.configureService(
          ConfigureServiceRequest(listOf(), ServiceType.SQUARE_DC))
    }

    scope(MiskCaller(user = "bob")).use {
      assertThatThrownBy {
        createBackfillAction.create("franklin", CreateBackfillRequest("abc"))
      }.isInstanceOf(BadRequestException::class.java)
    }
  }

  @Test
  fun created() {
    scope(MiskCaller(service = "franklin")).use {
      configureServiceAction.configureService(ConfigureServiceRequest(listOf(
          ConfigureServiceRequest.BackfillData("ChickenSandwich", listOf(), null, null, false)),
          ServiceType.SQUARE_DC))
    }
    scope(MiskCaller(user = "bob")).use {
      val response = createBackfillAction.create("franklin",
          CreateBackfillRequest("ChickenSandwich"))
      assertThat(response.statusCode).isEqualTo(HttpURLConnection.HTTP_MOVED_TEMP)

      transacter.transaction { session ->
        val run = queryFactory.newQuery<BackfillRunQuery>().uniqueResult(session)
        assertNotNull(run)
        assertThat(run.state).isEqualTo(BackfillState.PAUSED)
        assertThat(run.created_by_user).isEqualTo("bob")
        assertThat(run.approved_by_user).isNull()
        assertThat(run.approved_at).isNull()

        val instances = queryFactory.newQuery<RunInstanceQuery>()
            .backfillRunId(run.id)
            .list(session)
        assertThat(instances).hasSize(2)
      }
    }
  }

  fun scope(caller: MiskCaller) =
      scope.enter(mapOf(keyOf<MiskCaller>() to caller))
}
