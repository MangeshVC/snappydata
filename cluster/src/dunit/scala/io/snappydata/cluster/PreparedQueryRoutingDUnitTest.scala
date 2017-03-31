/*
 * Copyright (c) 2016 SnappyData, Inc. All rights reserved.
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

package io.snappydata.cluster

import java.sql.DriverManager
import com.pivotal.gemfirexd.internal.engine.distributed.utils.GemFireXDUtils
import io.snappydata.test.dunit.AvailablePortHelper
import org.apache.spark.Logging
import org.apache.spark.sql.SnappyContext

/**
 * Tests for query routing from JDBC client driver.
 */
class PreparedQueryRoutingDUnitTest(val s: String)
    extends ClusterManagerTestBase(s) with Logging {

  // set default batch size for this test
  private val default_chunk_size = GemFireXDUtils.DML_MAX_CHUNK_SIZE
  var serverHostPort = -1
  val tableName = "order_line_col"

  override def tearDown2(): Unit = {
    // reset the chunk size on lead node
    setDMLMaxChunkSize(default_chunk_size)
    super.tearDown2()
  }

  def setDMLMaxChunkSize(size: Long): Unit = {
    GemFireXDUtils.DML_MAX_CHUNK_SIZE = size
  }

  def insertRows(numRows: Int): Unit = {

    val conn = DriverManager.getConnection(
      "jdbc:snappydata://localhost:" + serverHostPort)

    val rows = (1 to numRows).toSeq
    val stmt = conn.createStatement()
    try {
      var i = 1
      rows.foreach(d => {
        stmt.addBatch(s"insert into $tableName values($i, $i, '$i')")
        i += 1
        if (i % 1000 == 0) {
          stmt.executeBatch()
          i = 0
        }
      })
      stmt.executeBatch()
      println(s"committed $numRows rows")
    } finally {
      stmt.close()
      conn.close()
    }
  }

  def query(): Unit = {
    val conn = DriverManager.getConnection(
      "jdbc:snappydata://localhost:" + serverHostPort)

    println(s"Connected to $serverHostPort")
    val stmt = conn.createStatement()
    var prepStatement: java.sql.PreparedStatement = null
    try {
      println("Prepared Statement: ")
      val qry = s"select ol_int_id, ol_int2_id, ol_str_id " +
          s" from $tableName " +
          s" where ol_int_id < ? " +
          s" and ol_int2_id > 100 " +
          s" and ol_str_id like ? " +
          s" limit 20" +
          s""

      val prepStatement = conn.prepareStatement(qry)
      prepStatement.setInt(1, 500)
      prepStatement.setString(2, "%0")
      val rs = prepStatement.executeQuery

      // val rs = stmt.executeQuery(qry)

      var index = 0
      while (rs.next()) {
        val i = rs.getInt(1)
        val j = rs.getInt(2)
        val s = rs.getString(3)
        println(s"row($index) $i $j $s ")
        index += 1
      }
      println("Number of rows read " + index)


      println("Statement: ")

      val qry2 = s"select ol_int_id, ol_int2_id, ol_str_id " +
          s" from $tableName " +
          s" where ol_int_id < 500 " +
          s" and ol_int2_id > 100 " +
          s" and ol_str_id LIKE '%0' " +
          s" limit 20" +
          s""
      val rs2 = stmt.executeQuery(qry2)
      var index2 = 0
      while (rs2.next()) {
        val i = rs2.getInt(1)
        val j = rs2.getInt(2)
        val s = rs2.getString(3)
        println(s"row($index2) $i $j $s ")
        index2 += 1
      }
      println("Number of rows read " + index2)
//      val reg = Misc.getRegionByPath("/APP/ORDER_LINE_COL", false).asInstanceOf[PartitionedRegion]
//      println("reg " + reg)
//      val itr = reg.getDataStore.getAllLocalBucketRegions.iterator()
//      val b1 = itr.next()
//      println("b = " + b1.getName + " and size = " + b1.size());
//
//      itr.next()
//      val b2 = itr.next()
//      println("b = " + b2.getName + " and size = " + b2.size());
      rs.close()

      //Thread.sleep(1000000)

    } finally {
      stmt.close()
      if (prepStatement != null) prepStatement.close()
      conn.close()
    }
  }

  def testPrepStatementRouting(): Unit = {
    serverHostPort = AvailablePortHelper.getRandomAvailableTCPPort
    vm2.invoke(classOf[ClusterManagerTestBase], "startNetServer", serverHostPort)
    println(s"network server started at $serverHostPort")

    val snc = SnappyContext(sc)
    snc.sql(s"create table $tableName (ol_int_id  integer," +
        s" ol_int2_id  integer, ol_str_id STRING) using column " +
        "options( partition_by 'ol_int_id, ol_int2_id', buckets '2')")

    insertRows(1000)

    // (1 to 5).foreach(d => query())
    query()
  }
}