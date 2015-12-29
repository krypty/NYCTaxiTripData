/*
 * Copyright 2015 Sanford Ryza, Uri Laserson, Sean Owen and Joshua Wills
 *
 * See LICENSE file for further information.
 */

import com.esri.core.geometry.Point
import org.apache.log4j.{Level, Logger}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SQLContext
import org.apache.spark.util.StatCounter
import org.apache.spark.{HashPartitioner, Partitioner, SparkConf, SparkContext}
import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTime, Duration}

import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag

case class Trip(
                 pickupTime: DateTime,
                 dropoffTime: DateTime,
                 pickupLoc: Point,
                 dropoffLoc: Point)

object Main extends Serializable {

  def main(args: Array[String]): Unit = {

    Logger.getLogger("org").setLevel(Level.WARN)
    Logger.getLogger("akka").setLevel(Level.WARN)

//        val TRIP_DATA_FILENAME = "taxidata/trip_data_1.csv"
    val TRIP_DATA_FILENAME = "taxidata/trip_data_first10000.csv"

    val conf = new SparkConf().setMaster("local[2]").setAppName("Simple Application")
    val sc = new SparkContext(conf)

    val sqlContext = new SQLContext(sc)

    val gjParser = new GeoJsonParser("conciles.geojson")
    val regions = gjParser.parse()
    val regionsManager = new RegionsManager(regions)

    val rdd = sqlContext.read
      .format("com.databricks.spark.csv")
      .option("header", "true") // Use first line of all files as header
      .load(TRIP_DATA_FILENAME).rdd

    //    rdd.foreach(x => println(x))

    def stringToDateTime(str: Any) = DateTimeFormat.forPattern("YYYY-MM-dd H:mm:s").parseDateTime(str.asInstanceOf[String])

    // contains only the cols we need (hack_license, pickup_datetime, dropoff_datetime, time_in_secs, pickup_longitude, pickup_latitude, dropoff_longitude, dropoff_latitude)
    var rddCleaned = rdd.map(r => (r(1), stringToDateTime(r(5)), stringToDateTime(r(6)), r(8), r(10), r(11), r(12), r(13)))

    val TRIP_TIME_LIMIT = 10800
    rddCleaned = rddCleaned.filter(r => r._4.asInstanceOf[String].toLong > 0 && r._4.asInstanceOf[String].toLong < TRIP_TIME_LIMIT)
    //    rddCleaned.foreach(r => println(r))


    val taxiDone = rddCleaned.map(r => (r._1.asInstanceOf[String], new Trip(r._2, r._3, new Point(r._5.asInstanceOf[String].toDouble, r._6.asInstanceOf[String].toDouble), new Point(r._7.asInstanceOf[String].toDouble, r._8.asInstanceOf[String].toDouble))))

    def secondaryKeyFunc(trip: Trip) = trip.pickupTime.getMillis
    val sessions = groupByKeyAndSortValues(taxiDone, secondaryKeyFunc, split, 30)

    //    sessions.foreach(r => println(r))
    sessions.cache()

    def regionDuration(t1: Trip, t2: Trip): (Int, Duration) = {
      val r = regionsManager.getRegion(t1.dropoffLoc)
      val d = new Duration(t1.dropoffTime, t2.pickupTime)
      (r, d)
    }

    val regionDurations: RDD[(Int, Duration)] =
      sessions.values.flatMap(trips => {
        val iter: Iterator[Seq[Trip]] = trips.sliding(2)
        val viter = iter.filter(_.size == 2)
        viter.map(p => regionDuration(p(0), p(1)))
      }).cache()

    val regionStats = regionDurations.filter {
      case (b, d) => d.getMillis >= 0
    }.mapValues(d => {
      val s = new StatCounter()
      s.merge(d.getStandardSeconds)
    })
      .reduceByKey((a, b) => a.merge(b)).collect()

    regionStats.foreach(println)


    gjParser.generateJsonFile(regionStats)
  }

  def point(longitude: String, latitude: String): Point = {
    new Point(longitude.toDouble, latitude.toDouble)
  }

  def split(t1: Trip, t2: Trip): Boolean = {
    val p1 = t1.pickupTime
    val p2 = t2.pickupTime
    val d = new Duration(p1, p2)
    d.getStandardHours >= 4
  }

  def groupByKeyAndSortValues[K: Ordering : ClassTag, V: ClassTag, S](
                                                                       rdd: RDD[(K, V)],
                                                                       secondaryKeyFunc: (V) => S,
                                                                       splitFunc: (V, V) => Boolean,
                                                                       numPartitions: Int): RDD[(K, List[V])] = {
    val presess = rdd.map {
      case (lic, trip) => {
        ((lic, secondaryKeyFunc(trip)), trip)
      }
    }
    val partitioner = new FirstKeyPartitioner[K, S](numPartitions)
    implicit val ordering: Ordering[(K, S)] = Ordering.by(_._1)
    presess.repartitionAndSortWithinPartitions(partitioner).mapPartitions(groupSorted(_, splitFunc))
  }

  def groupSorted[K, V, S](
                            it: Iterator[((K, S), V)],
                            splitFunc: (V, V) => Boolean): Iterator[(K, List[V])] = {
    val res = List[(K, ArrayBuffer[V])]()
    it.foldLeft(res)((list, next) => list match {
      case Nil => {
        val ((lic, _), trip) = next
        List((lic, ArrayBuffer(trip)))
      }
      case cur :: rest => {
        val (curLic, trips) = cur
        val ((lic, _), trip) = next
        if (!lic.equals(curLic) || splitFunc(trips.last, trip)) {
          (lic, ArrayBuffer(trip)) :: list
        } else {
          trips.append(trip)
          list
        }
      }
    }).map { case (lic, buf) => (lic, buf.toList) }.iterator
  }
}

class FirstKeyPartitioner[K1, K2](partitions: Int) extends Partitioner {
  val delegate = new HashPartitioner(partitions)

  override def numPartitions = delegate.numPartitions

  override def getPartition(key: Any): Int = {
    val k = key.asInstanceOf[(K1, K2)]
    delegate.getPartition(k._1)
  }
}