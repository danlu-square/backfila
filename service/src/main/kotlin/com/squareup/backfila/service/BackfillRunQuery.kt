package com.squareup.backfila.service

import misk.hibernate.Constraint
import misk.hibernate.Id
import misk.hibernate.Operator
import misk.hibernate.Order
import misk.hibernate.Query

interface BackfillRunQuery : Query<DbBackfillRun> {
  @Constraint("id")
  fun id(id: Id<DbBackfillRun>): BackfillRunQuery

  @Constraint("service_id")
  fun serviceId(serviceId: Id<DbService>): BackfillRunQuery

  @Constraint("state")
  fun state(state: BackfillState): BackfillRunQuery

  @Constraint("state", Operator.NE)
  fun stateNot(state: BackfillState): BackfillRunQuery

  @Order("id", asc = false)
  fun orderByIdDesc(): BackfillRunQuery
}
