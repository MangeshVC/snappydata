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
package io.snappydata.hydra.cluster;

import com.gemstone.gemfire.cache.query.Struct;
import com.gemstone.gemfire.cache.query.internal.types.StructTypeImpl;
import hydra.Log;
import hydra.RemoteTestModule;
import hydra.TestConfig;
import sql.sqlutil.ResultSetHelper;
import util.TestException;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import static hydra.Prms.totalTaskTimeSec;

public class SnappyConcurrencyTest extends SnappyTest {

  public static void runPointLookUpQueries() throws SQLException {
    Vector<String> queryVect = SnappyPrms.getPointLookUpQueryList();
    long totalTaskTime = TestConfig.tab().longAt(totalTaskTimeSec);
    Log.getLogWriter().info("SS - totalTaskTime : " + totalTaskTime);
    String query = null;
    Connection conn = getLocatorConnection();
    ResultSet rs;
    long startTime;
    long endTime = System.currentTimeMillis() + totalTaskTime;
    do {
      try {
        int queryNum = new Random().nextInt(queryVect.size());
        query = queryVect.elementAt(queryNum);
        Log.getLogWriter().info("SS - Executing query : " + query);
        rs = conn.createStatement().executeQuery(query);
        SnappyBB.getBB().getSharedCounters().increment(SnappyBB.numQueriesExecuted);
        int numQueries = (int) SnappyBB.getBB().getSharedCounters().read(SnappyBB.numQueriesExecuted);
        Log.getLogWriter().info("SS - numQueriesExecuted : " + numQueries);
        SnappyBB.getBB().getSharedCounters().increment(SnappyBB.numPointLookUpQueriesExecuted);
        int numPointLookUpQueries = (int) SnappyBB.getBB().getSharedCounters().read(SnappyBB
            .numPointLookUpQueriesExecuted);
        Log.getLogWriter().info("SS - numPointLookUpQueriesExecuted : " + numPointLookUpQueries);
        startTime = System.currentTimeMillis();
      } catch (SQLException se) {
        throw new TestException("Got exception while executing pointLookUp query:" + query, se);
      }
    } while (startTime < endTime);

    /*StructTypeImpl sti = ResultSetHelper.getStructType(rs);
    List<Struct> queryResult = ResultSetHelper.asList(rs, sti, false);
    Log.getLogWriter().info("SS - Result for query : " + query + "\n" + queryResult.toString());*/
    closeConnection(conn);
  }

  public static void runAnalyticalQueries() throws SQLException {
    Connection conn = getLocatorConnection();
    Vector<String> queryVect = SnappyPrms.getAnalyticalQueryList();
    long totalTaskTime = TestConfig.tab().longAt(totalTaskTimeSec);
    Log.getLogWriter().info("SS - totalTaskTime : " + totalTaskTime);
    String query = null;
    int queryNum = new Random().nextInt(queryVect.size());
    query = (String) queryVect.elementAt(queryNum);
    ResultSet rs;
    long startTime;
    long endTime = System.currentTimeMillis() + totalTaskTime;
    do {
      try {
        Log.getLogWriter().info("SS - Executing query : " + query);
        rs = conn.createStatement().executeQuery(query);
        SnappyBB.getBB().getSharedCounters().increment(SnappyBB.numQueriesExecuted);
        int numQueries = (int) SnappyBB.getBB().getSharedCounters().read(SnappyBB.numQueriesExecuted);
        Log.getLogWriter().info("SS - numQueriesExecuted : " + numQueries);
        SnappyBB.getBB().getSharedCounters().increment(SnappyBB.numAggregationQueriesExecuted);
        int numAggregationQueries = (int) SnappyBB.getBB().getSharedCounters().read(SnappyBB.numQueriesExecuted);
        Log.getLogWriter().info("SS - numAggregationQueriesExecuted : " + numAggregationQueries);
        startTime = System.currentTimeMillis();
      } catch (SQLException se) {
        throw new TestException("Got exception while executing Analytical query:" + query, se);
      }
    } while (startTime < endTime);
    /*StructTypeImpl sti = ResultSetHelper.getStructType(rs);
    List<Struct> queryResult = ResultSetHelper.asList(rs, sti, false);
    Log.getLogWriter().info("SS - Result for query : " + query + "\n" + queryResult.toString());*/
    closeConnection(conn);
  }

  public static void validateNumQueriesExecuted() throws SQLException {
    int numQueriesExecuted = (int) SnappyBB.getBB().getSharedCounters().read(SnappyBB.numQueriesExecuted);
    int numpointLookUpQueriesExecuted = (int) SnappyBB.getBB().getSharedCounters().read(SnappyBB
        .numPointLookUpQueriesExecuted);
    int numAggregationQueriesExecuted = (int) SnappyBB.getBB().getSharedCounters().read(SnappyBB.numAggregationQueriesExecuted);
    Log.getLogWriter().info("SS - Total number of queries executed : " + numQueriesExecuted);
    Log.getLogWriter().info("SS - Total number of pointLookUp queries executed : " +
        numpointLookUpQueriesExecuted);
    Log.getLogWriter().info("SS - Total number of analytical queries executed : " + numAggregationQueriesExecuted);
  }

}
