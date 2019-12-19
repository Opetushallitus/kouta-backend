package fi.oph.kouta.images

import java.io.{ByteArrayInputStream, DataInputStream, InputStream}

import fi.oph.kouta.auditlog.AuditLog
import fi.oph.kouta.servlet.{Authenticated, ImageSizeSpecs, MediaNotSupportedException, PayloadTooLargeException}
import fi.vm.sade.utils.slf4j.Logging
import javax.imageio.ImageIO
import javax.imageio.stream.ImageInputStream
import javax.servlet.http.HttpServletRequest
import javax.xml.parsers.SAXParser
import org.scalatra.servlet.ServletApiImplicits._

import scala.util.{Failure, Try}
import scala.xml.Elem
import scala.xml.factory.XMLLoader


object ImageService extends ImageService(S3Service, AuditLog)

class ImageService(s3Service: S3Service, auditLog: AuditLog) extends Logging {

  def storeTempImage(sizeSpecs: ImageSizeSpecs)(implicit authenticated: Authenticated, request: HttpServletRequest): String = {
    val length = checkLength(sizeSpecs.maxSize)
    val imageType = checkContentType()

    val imageData = readImageDataFromStream(request.inputStream, length)

    imageType match {
      case ImageType.Svg =>
        checkSvg(imageData)
      case _ =>
        checkImageFormatAndSize(imageType, imageData, sizeSpecs)
    }

    s3Service.storeTempImage(imageType, imageData)
  }

  private def checkSvg(imageData: Array[Byte]): Unit = {
    Try[Boolean] {
      val xml = DtdIgnoringXML.load(new ByteArrayInputStream(imageData))
      xml.label == "svg"
    }.toOption.filter(identity).getOrElse {
      throw MediaNotSupportedException(s"Tiedostoa ei voitu lukea image/svg+xml -kuvana")
    }
  }

  object DtdIgnoringXML extends XMLLoader[Elem] {
    override def parser: SAXParser = {
      val f = javax.xml.parsers.SAXParserFactory.newInstance()
      f.setNamespaceAware(false)
      f.setValidating(false)
      f.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
      f.newSAXParser()
    }
  }

  private def checkLength(maxSize: Int)(implicit request: HttpServletRequest): Int = request.contentLength match {
    case Some(l) if l > maxSize =>
      throw PayloadTooLargeException(s"Kuvan maksimikoko on $maxSize tavua")
    case Some(l) if l > 0 =>
      l.toInt
    case _ => throw new IllegalArgumentException(s"Virheellinen Content-Length -otsake")
  }

  private def checkContentType()(implicit request: HttpServletRequest): ImageType = request.contentType.map(ImageType.get) match {
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
