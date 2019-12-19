package fi.oph.kouta.servlet

import fi.oph.kouta.SwaggerPaths.registerPath
import fi.oph.kouta.images.ImageService
import org.scalatra.Ok

case class ImageSizeSpecs(maxSize: Int, minWidth: Int, minHeight: Int)

class UploadServlet(imageService: ImageService) extends KoutaServlet {

  def this() = this(ImageService)

  val teemakuvaSizes: ImageSizeSpecs = ImageSizeSpecs(maxSize = 2 * 1024 * 1024, minWidth = 1260, minHeight = 400)
  val logoSizes: ImageSizeSpecs      = ImageSizeSpecs(maxSize = 100 * 1024, minWidth = 100, minHeight = 100)

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
      |      responses:
      |        '200':
      |          description: Ok
      |""".stripMargin
  )
  post("/teemakuva") {
    implicit val authenticated: Authenticated = authenticate
    Ok("url" -> imageService.storeTempImage(teemakuvaSizes))
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
    implicit val authenticated: Authenticated = authenticate
    Ok("url" -> imageService.storeTempImage(logoSizes))
  }
}
