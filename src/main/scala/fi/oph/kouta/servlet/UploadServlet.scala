package fi.oph.kouta.servlet

import java.io._

import fi.oph.kouta.SwaggerPaths.registerPath
import fi.oph.kouta.indexing.{ImageType, S3Service}
import javax.imageio.ImageIO
import javax.imageio.stream.ImageInputStream
import org.scalatra.Ok

import scala.util.{Failure, Try}

case class ImageSizeSpecs(maxSize: Int, minWidth: Int, minHeight: Int)

class UploadServlet(s3Service: S3Service) extends KoutaServlet {

  def this() = this(S3Service)

  val teemakuvaSizes: ImageSizeSpecs = ImageSizeSpecs(maxSize = 2 * 1024 * 1024, minWidth = 1260, minHeight = 400)
  val logoSizes: ImageSizeSpecs  = ImageSizeSpecs(maxSize = 100 * 1024, minWidth = 100, minHeight = 100)

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
    storeTempImage(teemakuvaSizes)
  }

  registerPath(
    "/upload/logo",
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
    storeTempImage(logoSizes)
  }

  private def storeTempImage(sizeSpecs: ImageSizeSpecs) = {
    val length    = checkLength(sizeSpecs.maxSize)
    val imageType = checkContentType()

    val imageData = readImageDataFromStream(request.inputStream, length)

    imageType match {
      case ImageType.Svg =>
        checkSvg(imageData)
      case _ =>
        checkImageFormatAndSize(imageType, imageData, sizeSpecs.minWidth, sizeSpecs.minHeight)
    }

    Ok("url" -> s3Service.storeTempImage(imageType, imageData))
  }

  private def checkSvg(imageData: Array[Byte]): Unit = {
    Try[Boolean] {
      val xml = DtdIgnoringXML.load(new ByteArrayInputStream(imageData))
      xml.label == "svg"
    }.toOption.filter(identity).getOrElse {
      throw MediaNotSupportedException(s"Tiedostoa ei voitu lukea image/svg+xml -kuvana")
    }
  }

  import javax.xml.parsers.SAXParser

  import scala.xml.Elem
  import scala.xml.factory.XMLLoader

  object DtdIgnoringXML extends XMLLoader[Elem] {
    override def parser: SAXParser = {
      val f = javax.xml.parsers.SAXParserFactory.newInstance()
      f.setNamespaceAware(false)
      f.setValidating(false)
      f.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
      f.newSAXParser()
    }
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

  private def checkImageFormatAndSize(imageType: ImageType, imageData: Array[Byte], sizeSpecs: ImageSizeSpecs): Unit = {
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

    if (width < sizeSpecs.minWidth || height < sizeSpecs.minHeight) {
      throw new IllegalArgumentException(
        s"Kuva on väärän kokoinen ($width x $height), kun minimikoko on ${sizeSpecs.minWidth} x ${sizeSpecs.minHeight}"
      )
    }
  }
}
