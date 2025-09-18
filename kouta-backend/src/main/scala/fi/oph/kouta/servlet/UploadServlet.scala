package fi.oph.kouta.servlet

import fi.oph.kouta.SwaggerPaths.registerPath
import fi.oph.kouta.images.{ImageService, ImageServlet, S3ImageService}
import org.scalatra.Ok

trait ImageSizeSpecs {
  def maxSize: Int
  def minWidth: Int
  def minHeight: Int
}

case class ImageWithMinSizeSpecs(maxSize: Int, minWidth: Int, minHeight: Int) extends ImageSizeSpecs

case class ImageWithMaxSizeSpecs(maxSize: Int, minWidth: Int, minHeight: Int, maxWidth: Int, maxHeight: Int) extends ImageSizeSpecs

class UploadServlet(val s3ImageService: ImageService) extends KoutaServlet with ImageServlet {

  def this() = this(S3ImageService)

  val teemakuvaSizes: ImageWithMinSizeSpecs = ImageWithMinSizeSpecs(maxSize = 2 * 1000 * 1000, minWidth = 1260, minHeight = 400)
  val smallerTeemakuvaSizes: ImageWithMinSizeSpecs = ImageWithMinSizeSpecs(maxSize = 2 * 1000 * 1000, minWidth = 630, minHeight = 200)
  val logoSizes: ImageWithMinSizeSpecs = ImageWithMinSizeSpecs(maxSize = 100 * 1000, minWidth = 0, minHeight = 0)
  val iconSizes: ImageWithMaxSizeSpecs = ImageWithMaxSizeSpecs(maxSize = 2 * 1000 * 1000, minWidth = 0, minHeight = 0, maxWidth = 500, maxHeight = 500)

  registerPath("/upload/teemakuva",
    """    post:
      |      summary: Tallenna teemakuva
      |      operationId: Tallenna teemakuva
      |      description: Tallenna teemakuva väliaikaiseen sijaintiin.
      |        Teemakuva siirretään lopulliseen sijaintiinsa, kun se asetetaan jonkin objektin teemakuvaksi.
      |      tags:
      |        - Upload
      |      requestBody:
      |        content:
      |          'image/jpeg':
      |            schema:
      |              type: string
      |              format: binary
      |          'image/png':
      |            schema:
      |              type: string
      |              format: binary
      |      parameters:
      |        - in: query
      |          name: isSmallTeemakuva
      |          schema:
      |            type: boolean
      |            default: false
      |          required: false
      |          description: Palautetaanko myös mahdollisesti poistettu koulutus
      |      responses:
      |        '200':
      |          description: Ok
      |""".stripMargin
  )
  post("/teemakuva") {
    implicit val authenticated: Authenticated = authenticate()
    val isSmallTeemakuva = params.getOrElse("isSmallTeemakuva", "false").toBoolean
    val kuvaSizesSpec = if (isSmallTeemakuva) smallerTeemakuvaSizes else teemakuvaSizes
    Ok("url" -> storeTempImage(kuvaSizesSpec))
  }

  registerPath("/upload/icon",
    """    post:
      |      summary: Tallenna kuvake
      |      operationId: Tallenna kuvake
      |      description: Tallenna kuvake väliaikaiseen sijaintiin.
      |        Kuvake siirretään lopulliseen sijaintiinsa, kun se asetetaan jonkin objektin teemakuvaksi.
      |      tags:
      |        - Upload
      |      requestBody:
      |        content:
      |          'image/jpeg':
      |            schema:
      |              type: string
      |              format: binary
      |          'image/png':
      |            schema:
      |              type: string
      |              format: binary
      |      responses:
      |        '200':
      |          description: Ok
      |""".stripMargin
  )
  post("/icon") {
    implicit val authenticated: Authenticated = authenticate()
    Ok("url" -> storeTempImage(iconSizes))
  }

  registerPath("/upload/logo",
    s"""    post:
       |      summary: Tallenna logo
       |      operationId: Tallenna logo
       |      description: Tallenna oppilaitoksen logo väliaikaiseen sijaintiin.
       |        Logo siirretään lopulliseen sijaintiinsa, kun se asetetaan oppilaitoksen logoksi.
       |      tags:
       |        - Upload
       |      requestBody:
       |        content:
       |          'image/jpeg':
       |            schema:
       |              type: string
       |              format: binary
       |          'image/png':
       |            schema:
       |              type: string
       |              format: binary
       |          'image/svg+xml':
       |            schema:
       |              type: string
       |              format: binary
       |      responses:
       |        '200':
       |          description: Ok
       |""".stripMargin
  )
  post("/logo") {
    implicit val authenticated: Authenticated = authenticate()
    Ok("url" -> storeTempImage(logoSizes))
  }
}
