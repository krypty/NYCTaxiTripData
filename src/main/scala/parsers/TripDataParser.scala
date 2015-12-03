import org.apache.spark.sql.SQLContext
import org.apache.spark.{SparkContext, SparkConf}

object TripDataParser {
  def main(args: Array[String]) {
    println("Hello World")

    val TRIP_DATA_FILENAME = "taxidata/trip_data_first100.csv"

    val conf = new SparkConf().setMaster("local[2]").setAppName("Simple Application")
    val sc = new SparkContext(conf)

    val sqlContext = new SQLContext(sc)

    val df = sqlContext.read
      .format("com.databricks.spark.csv")
      .option("header", "true") // Use first line of all files as header
      .load(TRIP_DATA_FILENAME).registerTempTable("yolo_table")

    // print the csv structure
    //    df.printSchema()

    //    queries can be made like this
    //    df.groupBy("vendor_id").count().show()

    // or using raw sql
    val records = sqlContext.sql( """select * from yolo_table WHERE hack_license = '7CE849FEF67514F080AF80D990F7EF7F'""").take(5)

    // print query results
    //    sqlContext.sql( """select * from yolo_table WHERE hack_license = '7CE849FEF67514F080AF80D990F7EF7F'""").show()

    // get query results
    val colNumber = 5
    println(records.foreach(x => println(x.getString(colNumber))))

  }
}
