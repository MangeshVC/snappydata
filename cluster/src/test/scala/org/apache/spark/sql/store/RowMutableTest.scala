/*
 * Copyright (c) 2017 SnappyData, Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You
 * may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License. See accompanying
 * LICENSE file.
 */
package org.apache.spark.sql.store

import io.snappydata.SnappyFunSuite

/**
 * Update, delete tests for row tables.
 */
class RowMutableTest extends SnappyFunSuite {

  test("Test the update table ") {
    snc.sql("CREATE TABLE RowTableUpdate(CODE INT,DESCRIPTION varchar(100)) " +
        "USING row")

    snc.sql("insert into RowTableUpdate values (5,'test')")
    snc.sql("insert into RowTableUpdate values (6,'test1')")

    val df1 = snc.sql("select DESCRIPTION from RowTableUpdate " +
        "where DESCRIPTION = 'test'")
    assert(df1.count() == 1)

    val d1 = snc.sql("select * from RowTableUpdate")
    assert(d1.count() == 2)

    snc.sql("CREATE TABLE RowTableUpdate2 (CODE INT PRIMARY KEY, DESCRIPTION varchar(100)) " +
        "USING row AS (select * from  RowTableUpdate)")

    val d2 = snc.sql("select * from  RowTableUpdate2")
    assert(d2.count() == 2)

    snc.sql("update RowTableUpdate2 set DESCRIPTION ='No#complaints' " +
        "where CODE = 5").collect()

    val df2 = snc.sql("select DESCRIPTION from RowTableUpdate2 " +
        "where DESCRIPTION = 'No#complaints' ")
    assert(df2.count() == 1)

    val df3 = snc.sql("select DESCRIPTION from RowTableUpdate2 " +
        "where DESCRIPTION in ('No#complaints', 'test1') ")
    assert(df3.count() == 2)

    snc.dropTable("RowTableUpdate")
    snc.dropTable("RowTableUpdate2")
  }
}
