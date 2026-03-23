package com.u1.slicer.shared.database

import app.cash.sqldelight.Transacter
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import com.u1.slicer.shared.database.shared.newInstance
import com.u1.slicer.shared.database.shared.schema
import kotlin.Unit

public interface U1SlicerDatabase : Transacter {
  public val filamentQueriesQueries: FilamentQueriesQueries

  public val sliceJobQueriesQueries: SliceJobQueriesQueries

  public companion object {
    public val Schema: SqlSchema<QueryResult.Value<Unit>>
      get() = U1SlicerDatabase::class.schema

    public operator fun invoke(driver: SqlDriver): U1SlicerDatabase =
        U1SlicerDatabase::class.newInstance(driver)
  }
}
