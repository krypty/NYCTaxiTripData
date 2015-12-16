
import java.io.{InputStreamReader, BufferedReader, PrintWriter}

import play.api.libs.json._
import play.api.libs.json.monad.syntax._
import play.api.libs.json.extensions._

import com.esri.core.geometry._
import org.apache.spark.rdd.RDD
import scala.collection.mutable.HashMap
import org.apache.spark.sql.Row

class GeoJsonParser(filePath: String) {
  val stream = this.getClass.getResourceAsStream(filePath)

  val inputStr = scala.io.Source.fromInputStream(stream).mkString

  val jsonObj: JsValue = Json.parse(inputStr)

  def parse(): HashMap[Int, Polygon] = {
    val regions = new HashMap[Int, Polygon]()
    try {
      val features = (jsonObj \ "features").as[JsArray]

      for (feature <- features.value) {

        val counDist : Int = (feature \ "properties" \ "CounDist").as[Int]
        val geometryString = (feature \ "geometry").toString()
        val mapGeom = OperatorImportFromGeoJson.local().execute(GeoJsonImportFlags.geoJsonImportDefaults, Geometry.Type.Polygon, geometryString, null)
        val polygon = mapGeom.getGeometry().asInstanceOf[Polygon]

        regions += counDist -> polygon

      }
    } finally {
      if (stream != null)
        stream.close()
    }
    return regions
  }

  def generateJsonFile(regionDataRDD: RDD[Row]): Unit = {
    val regionData = regionDataRDD.collect()

    var newJsonObj = jsonObj

    val features = (jsonObj \ "features").as[JsArray]
    for (feature <- features.value)
    {

      val id = (feature \ "id").as[Int]

      val properties = feature \ "properties"

      val counDist = (properties \ "CounDist").as[Int]

      val dataLine = regionData.find(x => x(0).equals(counDist))

      //add the new properties
      if (dataLine != None) {
        newJsonObj = newJsonObj.set(
          (__ \ 'features)(id) \ 'properties \ 'name -> JsNumber(counDist),
          (__ \ 'features)(id) \ 'properties \ 'time -> JsNumber(dataLine.get(1).toString.toDouble),
          (__ \ 'features)(id) \ 'properties \ 'density -> JsNumber(dataLine.get(2).toString.toDouble)
        )
      }
      else
      {
        newJsonObj = newJsonObj.set(
          (__ \ 'features)(id) \ 'properties \ 'name -> JsNumber(counDist),
          (__ \ 'features)(id) \ 'properties \ 'time -> JsNumber(0),
          (__ \ 'features)(id) \ 'properties \ 'density -> JsNumber(0)
        )
      }
    }

    new PrintWriter("frontend/city_council.js") { write("var statesData = " + newJsonObj.toString); close }

  }
}

