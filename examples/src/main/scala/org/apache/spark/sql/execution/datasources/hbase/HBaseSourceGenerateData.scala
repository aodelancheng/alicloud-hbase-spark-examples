/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * File modified by Hortonworks, Inc. Modifications are also licensed under
 * the Apache Software License, Version 2.0.
 */

package org.apache.spark.sql.execution.datasources.hbase

import java.io.IOException
import java.util.Random

import org.apache.spark.sql.execution.datasources.hbase._
import org.apache.spark.sql.{DataFrame, SparkSession}

case class HBaseRecord(
                        col0: String,
                        col1: Boolean,
                        col2: Double,
                        col3: Float,
                        col4: Int,
                        col5: Long,
                        col6: Short,
                        col7: String,
                        col8: Byte)

object HBaseRecord {
  def apply(i: Int): HBaseRecord = {
    val s = s"""row${"%010d".format(i)}"""
    HBaseRecord(s,
      i % 2 == 0,
      i.toDouble,
      i.toFloat,
      i,
      i.toLong,
      i.toShort,
      s"String$i extra",
      i.toByte)
  }
}

object HBaseSourceGenerateData {
  val cat =
    s"""{
       |"table":{"namespace":"default", "name":"shcExampleBigTable", "tableCoder":"PrimitiveType"},
       |"rowkey":"key",
       |"columns":{
       |"col0":{"cf":"rowkey", "col":"key", "type":"string"},
       |"col1":{"cf":"cf1", "col":"col1", "type":"boolean"},
       |"col2":{"cf":"cf2", "col":"col2", "type":"double"},
       |"col3":{"cf":"cf3", "col":"col3", "type":"float"},
       |"col4":{"cf":"cf4", "col":"col4", "type":"int"},
       |"col5":{"cf":"cf5", "col":"col5", "type":"bigint"},
       |"col6":{"cf":"cf6", "col":"col6", "type":"smallint"},
       |"col7":{"cf":"cf7", "col":"col7", "type":"string"},
       |"col8":{"cf":"cf8", "col":"col8", "type":"tinyint"}
       |}
       |}""".stripMargin

  val cat1 =
    s"""{
       |"table":{"namespace":"default", "name":"shcExampleBigTable1", "tableCoder":"PrimitiveType"},
       |"rowkey":"key",
       |"columns":{
       |"col0":{"cf":"rowkey", "col":"key", "type":"string"},
       |"col1":{"cf":"cf1", "col":"col1", "type":"boolean"},
       |"col2":{"cf":"cf2", "col":"col2", "type":"double"},
       |"col3":{"cf":"cf3", "col":"col3", "type":"float"},
       |"col4":{"cf":"cf4", "col":"col4", "type":"int"},
       |"col5":{"cf":"cf5", "col":"col5", "type":"bigint"},
       |"col6":{"cf":"cf6", "col":"col6", "type":"smallint"},
       |"col7":{"cf":"cf7", "col":"col7", "type":"string"},
       |"col8":{"cf":"cf8", "col":"col8", "type":"tinyint"}
       |}
       |}""".stripMargin

  def main(args: Array[String]) {
    val spark = SparkSession.builder()
      .appName("HBaseSourceExample2")
      .getOrCreate()
    try {
      val sc = spark.sparkContext
      val sqlContext = spark.sqlContext

      import sqlContext.implicits._

      def withCatalog(cat: String): DataFrame = {
        sqlContext
          .read
          .options(Map(HBaseTableCatalog.tableCatalog -> cat))
          .format("org.apache.spark.sql.execution.datasources.hbase")
          .load()
      }

      //1. write data
      val random: Random = new Random
      val data = sc.parallelize((0 to 100000000), 20).map { i => math.abs(random.nextInt()) }.map { i =>
        HBaseRecord(i)
      }

      data.toDF.write.options(
        Map(HBaseTableCatalog.tableCatalog -> cat, HBaseTableCatalog.newTable -> "5"))
        .format("org.apache.spark.sql.execution.datasources.hbase")
        .save()

      // for testing connection sharing only
      data.toDF.write.options(
        Map(HBaseTableCatalog.tableCatalog -> cat1, HBaseTableCatalog.newTable -> "5"))
        .format("org.apache.spark.sql.execution.datasources.hbase")
        .save()

    } catch {
      case e: Throwable => println(e.getMessage)
    } finally {
      spark.stop()
    }

  }
}