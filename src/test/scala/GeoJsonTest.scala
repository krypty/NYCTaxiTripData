
import com.esri.core.geometry.Point
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterAll, FunSuite}

/**
  * Unit test for GeoJsonParser
  */
@RunWith(classOf[JUnitRunner])
class GeoJsonTest extends FunSuite with BeforeAndAfterAll {

  test("'neuchatel' should not be in New York") {

    val gjParser = new GeoJsonParser("conciles.geojson")
    val regions = gjParser.parse()

    val regionsManager = new RegionsManager(regions)

    val neuchatel = new Point(6.930567, 46.990281)

    val res = regionsManager.getRegion(neuchatel)
    assert(res == -1, "neuchatel region should equal to -1 (no region)")
  }

  test("liberty Statue should be in New York, region 1") {

    val gjParser = new GeoJsonParser("conciles.geojson")
    val regions = gjParser.parse()

    val regionsManager = new RegionsManager(regions)

    val libertyStatue = new Point(-74.044444, 40.689167)

    val res = regionsManager.getRegion(libertyStatue)
    assert(res == 1, "liberty statue region should equal to 1")
  }

}



