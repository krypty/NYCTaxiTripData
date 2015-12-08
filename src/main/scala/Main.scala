import com.esri.core.geometry.Point

object Main {
  def main(args: Array[String]) {


    val regions = GeoJsonParser.parse("conciles.geojson")

    val regionsManager = new RegionsManager(regions)

    val libertyStatue = new Point(-74.044444, 40.689167)
    val neuchatel = new Point(6.930567, 46.990281)

    println(regionsManager.getRegion(libertyStatue))
    println(regionsManager.getRegion(neuchatel))

  }
}
