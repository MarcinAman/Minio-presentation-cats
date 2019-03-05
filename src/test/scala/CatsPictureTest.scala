import org.json4s.MappingException
import org.scalatest.{FlatSpec, Matchers}

class CatsPictureTest extends FlatSpec with Matchers {
  "fromJson" should "create object from valid json" in {
    val json =
      """
        |[
        | {
        |   "breeds":["bear"],
        |   "id":"4qm",
        |   "url":"https://cdn2.thecatapi.com/images/4qm.gif"
        | }
        |]
      """.stripMargin

    CatsPicture.fromJson(json)  should equal(CatsPicture(Seq("bear"), "4qm", "https://cdn2.thecatapi.com/images/4qm.gif"))
  }

  "fromJson" should "create object from another valid json" in {
    val json =
      """
        |[
        | {
        |   "breeds":[],
        |   "id":"4qm",
        |   "url":"https://cdn2.thecatapi.com/images/4qm.gif"
        | }
        |]
      """.stripMargin

    CatsPicture.fromJson(json)  should equal(CatsPicture(Seq.empty, "4qm", "https://cdn2.thecatapi.com/images/4qm.gif"))
  }

  "fromJson" should "throw exception from json without a field" in {
    val json =
      """
        |[
        | {
        |   "breeds":[],
        |   "url":"https://cdn2.thecatapi.com/images/4qm.gif"
        | }
        |]
      """.stripMargin

    assertThrows[MappingException] {
      CatsPicture.fromJson(json)
    }
  }
}
