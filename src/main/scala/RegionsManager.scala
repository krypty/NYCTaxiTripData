import com.esri.core.geometry.{GeometryEngine, Point, SpatialReference, Polygon}

import scala.collection.mutable

class RegionsManager(val regions:mutable.HashMap[Int, Polygon]) extends Serializable{

  val defaultSpatialReference = SpatialReference.create(4326)

  def getRegion(point:Point): Int = {
    val region = regions.find(region => GeometryEngine.contains(region._2, point, defaultSpatialReference))
    if (region.isDefined)
      return region.get._1
    else return -1
  }

}
