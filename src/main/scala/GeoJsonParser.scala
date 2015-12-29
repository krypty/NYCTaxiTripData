
import java.io.PrintWriter

import com.esri.core.geometry._
import org.apache.spark.util.StatCounter
import play.api.libs.json._
import play.api.libs.json.extensions._

import scala.collection.mutable.HashMap

class GeoJsonParser(filePath: String) {
  val stream = this.getClass.getResourceAsStream(filePath)

  val inputStr = scala.io.Source.fromInputStream(stream).mkString

  val jsonObj: JsValue = Json.parse(inputStr)

  def parse(): HashMap[Int, Polygon] = {
    val regions = new HashMap[Int, Polygon]()
    try {
      val features = (jsonObj \ "features").as[JsArray]

      for (feature <- features.value) {

        val counDist: Int = (feature \ "properties" \ "CounDist").as[Int]
        val geometryString = (feature \ "geometry").toString()
        val mapGeom = OperatorImportFromGeoJson.local().execute(GeoJsonImportFlags.geoJsonImportDefaults, Geometry.Type.Polygon, geometryString, null)
        val polygon = mapGeom.getGeometry.asInstanceOf[Polygon]

        regions += counDist -> polygon

      }
    } finally {
      if (stream != null)
        stream.close()
    }
    return regions
  }

  def generateJsonFile(regionStats: Array[(Int, StatCounter)]): Unit = {

    val maxMean = regionStats.maxBy(r => r._2.mean)._2.mean
    println("max: " + maxMean)

    var newJsonObj = jsonObj

    val features = (jsonObj \ "features").as[JsArray]
    for (feature <- features.value) {

      val id = (feature \ "id").as[Int]

      val properties = feature \ "properties"

      val counDist = (properties \ "CounDist").as[Int]

      val dataLine = regionStats.find(x => x._1.equals(counDist))

      //add the new properties
      if (dataLine.isDefined) {
        newJsonObj = newJsonObj.set(
          (__ \ 'features) (id) \ 'properties \ 'name -> JsNumber(counDist),
          (__ \ 'features) (id) \ 'properties \ 'mean -> JsNumber(dataLine.get._2.mean),
          (__ \ 'features) (id) \ 'properties \ 'density -> JsNumber((dataLine.get._2.mean / maxMean) * 100)
        )
      }
      else {
        newJsonObj = newJsonObj.set(
          (__ \ 'features) (id) \ 'properties \ 'name -> JsNumber(counDist),
          (__ \ 'features) (id) \ 'properties \ 'mean -> JsNumber(0),
          (__ \ 'features) (id) \ 'properties \ 'density -> JsNumber(100)
        )
      }
    }

    new PrintWriter("frontend/city_council.js") {
      write("var statesData = " + newJsonObj.toString)
      close
    }

  }
}

