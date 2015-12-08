
import com.esri.core.geometry._

import scala.collection.mutable.HashMap
import spray.json._
import DefaultJsonProtocol._
import scala.io.Source
import java.io.File

object GeoJsonParser {

  def parse(filePath:String):HashMap[Int, Polygon] = {

    val stream = this.getClass.getResourceAsStream(filePath)
    try {
      val json = Source.fromInputStream(stream).mkString

      val jsonAst = json.parseJson


      val regions = new HashMap[Int, Polygon]()
      val features = jsonAst.asJsObject.getFields("features")(0).convertTo[JsArray].elements.foreach { feature =>

        val counDist = feature.asJsObject.getFields("properties")(0).asJsObject.getFields("CounDist")(0).toString().toInt

        val geometryString = feature.asJsObject.getFields("geometry")(0).toString()
        val mapGeom = OperatorImportFromGeoJson.local().execute(GeoJsonImportFlags.geoJsonImportDefaults, Geometry.Type.Polygon, geometryString, null);
        val polygon = mapGeom.getGeometry().asInstanceOf[Polygon];

        regions += counDist -> polygon
      }

      return regions
    } finally {
      if (stream != null)
        stream.close()
    }
  }

}

