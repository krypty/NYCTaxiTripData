
import com.esri.core.geometry.Point
import org.apache.log4j.{Logger, Level}
import org.apache.spark.sql.SQLContext
import org.apache.spark.{SparkContext, SparkConf}

object TripDataParser {
  def main(args: Array[String]) {
    println("Hello World")

    // reduce spark verbosity
    Logger.getLogger("org").setLevel(Level.WARN)
    Logger.getLogger("akka").setLevel(Level.WARN)

    val TRIP_DATA_FILENAME = "taxidata/trip_data_1.csv"
    //    val TRIP_DATA_FILENAME = "taxidata/trip_data_first10000.csv"

    val conf = new SparkConf().setMaster("local[2]").setAppName("Simple Application")
    val sc = new SparkContext(conf)

    val sqlContext = new SQLContext(sc)

    val df = sqlContext.read
      .format("com.databricks.spark.csv")
      .option("header", "true") // Use first line of all files as header
      .load(TRIP_DATA_FILENAME).registerTempTable("tripdata_table")


    val gjParser = new GeoJsonParser("conciles.geojson")
    val regions = gjParser.parse()
    val regionsManager = new RegionsManager(regions)

    def getRegion(longitude: Double, latitude: Double) = {
      regionsManager.getRegion(new Point(longitude, latitude))
    }
    sqlContext.udf.register("getRegion", getRegion(_: Double, _: Double))

    // or using raw sql
    sqlContext.sql( """SELECT *, getRegion(pickup_longitude, pickup_latitude) as pickup_region FROM tripdata_table """).registerTempTable("tripdata_region_table")

    val sumInSecs = sqlContext.sql( """SELECT sum(trip_time_in_secs) FROM tripdata_region_table""").collect()(0)(0).asInstanceOf[Double]
    println("sum in secs: " + sumInSecs)
    sqlContext.sql( """SELECT pickup_region, sum(trip_time_in_secs) as total_time_in_secs FROM tripdata_region_table GROUP BY pickup_region """).registerTempTable("region_time_table")

    val sumInSecsStr: String = "%.1f".format(sumInSecs)
    val rdd = sqlContext.sql( s"""SELECT pickup_region, total_time_in_secs, (total_time_in_secs / $sumInSecsStr) * 100 as total_time_in_percent FROM region_time_table ORDER BY total_time_in_secs DESC, pickup_region ASC """).rdd

    println("rdd count : " + rdd.count())

    gjParser.generateJsonFile(rdd)
  }
}
