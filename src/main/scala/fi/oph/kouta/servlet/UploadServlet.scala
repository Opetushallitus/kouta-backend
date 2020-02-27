package fi.oph.kouta.servlet

import java.io._

import fi.oph.kouta.SwaggerPaths.registerPath
import fi.oph.kouta.indexing.{ImageType, S3Service}
import javax.imageio.ImageIO
import javax.imageio.stream.ImageInputStream
import org.scalatra.Ok

import scala.util.{Failure, Try}

class UploadServlet(s3Service: S3Service, maxSize: Int = 2 * 1024 * 1024) extends KoutaServlet {

  def this() = this(S3Service)

  val teemakuvaMinWidth  = 1260
  val teemakuvaMinHeight = 400

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
    val length      = checkLength(maxSize)
    val contentType = checkContentType()

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

  private def checkContentType(): ImageType = request.contentType.map(ImageType.get) match {
    case Some(Some(t)) => t
    case None          => throw new IllegalArgumentException("Otsake Content-Type puuttuu")
    case Some(None)    =>
      throw MediaNotSupportedException(
        s"Kuvamuoto ${request.contentType} ei ole tuettu. Sallitut muodot ovat: ${ImageType.allowedTypes.mkString(", ")}"
      )
  }

  private def readImageDataFromStream(inputStream: InputStream, length: Int): Array[Byte] = {
    val imageData = new Array[Byte](length)
    val dis       = new DataInputStream(inputStream)
    dis.readFully(imageData, 0, length)
    dis.close()
    imageData
  }

  private def checkImageFormatAndSize(imageType: ImageType, imageData: Array[Byte]): Unit = {
    ImageIO.setUseCache(false)
    val mimeReaders = ImageIO.getImageReadersByMIMEType(imageType.contentType)
    val reader = mimeReaders.next()
    val image = Try {
      reader.setInput(ImageIO.createImageInputStream(new ByteArrayInputStream(imageData)))
      reader.read(reader.getMinIndex)
    }.recoverWith {
      case e: Exception =>
        logger.warn("Exception while reading image", e)
        Failure(MediaNotSupportedException(s"Tiedostoa ei voitu lukea ${imageType.contentType} -kuvana"))
    }

    reader.getInput match {
      case stream: ImageInputStream => stream.close()
      case _ =>
    }
    reader.dispose()

    val (width, height) = (image.get.getWidth, image.get.getHeight)

    if (width < teemakuvaMinWidth || height < teemakuvaMinHeight) {
      throw new IllegalArgumentException(
        s"Kuva on väärän kokoinen ($width x $height), kun minimikoko on $teemakuvaMinWidth x $teemakuvaMinHeight"
      )
    }
  }

}
