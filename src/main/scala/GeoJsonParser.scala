
import com.esri.core.geometry._
import org.apache.spark.rdd.RDD

import scala.collection.mutable.HashMap
import spray.json._
import DefaultJsonProtocol._
import scala.io.Source

class GeoJsonParser(filePath: String) {
  val stream = this.getClass.getResourceAsStream(filePath)
  val json = Source.fromInputStream(stream).mkString
  val jsonAst = json.parseJson

  def parse(): HashMap[Int, Polygon] = {
    val regions = new HashMap[Int, Polygon]()
    try {
      val features = jsonAst.asJsObject.getFields("features")(0).convertTo[JsArray].elements.foreach { feature =>

        val counDist = feature.asJsObject.getFields("properties")(0).asJsObject.getFields("CounDist")(0).toString().toInt

        val geometryString = feature.asJsObject.getFields("geometry")(0).toString()
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

  def generateJsonFile(regionDataRDD: RDD): Unit = {
    val regionData = regionDataRDD.collect()

    val features = jsonAst.asJsObject.getFields("features")(0).convertTo[JsArray].elements.foreach { feature =>
      var properties = feature.asJsObject.getFields("properties")(0).asJsObject

      jsonAst.fromJson[Address].
    }

  }

}

