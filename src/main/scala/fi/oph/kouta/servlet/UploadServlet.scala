package fi.oph.kouta.servlet

import java.io._

import fi.oph.kouta.SwaggerPaths.registerPath
import fi.oph.kouta.indexing.S3Service
import javax.imageio.ImageIO
import org.scalatra.Ok

import scala.util.{Failure, Try}

case class PayloadTooLargeException(message: String) extends RuntimeException(message)

case class MediaNotSupportedException(message: String) extends RuntimeException(message)

class UploadServlet(s3Service: S3Service, maxSize: Int = 2 * 1024 * 1024) extends KoutaServlet {

  def this() = this(S3Service)

  val themeMinWidth  = 1260
  val themeMinHeight = 400

  registerPath("/upload/theme-image",
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
  post("/theme-image") {
    val length      = checkLength(maxSize)
    val contentType = checkContentType(s3Service.allowedImageTypes)

    val imageData       = readImageDataFromStream(request.inputStream, length)
    checkImageFormatAndSize(contentType, imageData)

    Ok("url" -> s3Service.storeTempImage(contentType, imageData))
  }

  private def checkLength(maxSize: Int): Int = request.contentLength match {
    case Some(l) if l > maxSize =>
      throw PayloadTooLargeException(s"Kuvan maksimikoko on $maxSize tavua")
    case Some(l) if l > 0 =>
      l.toInt
    case _ => throw new IllegalArgumentException(s"Virheellinen Content-Length -otsake")
  }

  private def checkContentType(allowedImageTypes: Set[String]): String = request.contentType match {
    case Some(t) if allowedImageTypes.contains(t) => t
    case None    => throw new IllegalArgumentException("Otsake Content-Type puuttuu")
    case Some(t) =>
      throw MediaNotSupportedException(
        s"Kuvamuoto $t ei ole tuettu. Sallitut muodot ovat: ${allowedImageTypes.mkString(", ")}"
      )
  }

  private def readImageDataFromStream(inputStream: InputStream, length: Int): Array[Byte] = {
    val imageData = new Array[Byte](length)
    val dis       = new DataInputStream(inputStream)
    dis.readFully(imageData, 0, length)
    dis.close()
    imageData
  }

  private def checkImageFormatAndSize(contentType: String, imageData: Array[Byte]): Unit = {
    val (width, height) = Try {
      val mimeReaders = ImageIO.getImageReadersByMIMEType(contentType)
      val reader      = mimeReaders.next()
      reader.setInput(ImageIO.createImageInputStream(new ByteArrayInputStream(imageData)))
      val image = reader.read(reader.getMinIndex)

      (image.getWidth, image.getHeight)
    }.recoverWith {
      case _: Exception =>
        Failure(MediaNotSupportedException(s"Tiedostoa ei voitu lukea $contentType -kuvana"))
    }.get

    if (width < themeMinWidth || height < themeMinHeight) {
      throw new IllegalArgumentException(
        s"Kuva on väärän kokoinen ($width x $height), kun minimikoko on $themeMinWidth x $themeMinHeight"
      )
    }
  }

}
