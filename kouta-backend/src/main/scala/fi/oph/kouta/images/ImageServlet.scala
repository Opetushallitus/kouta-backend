package fi.oph.kouta.images

import fi.oph.kouta.servlet._

import java.io.{ByteArrayInputStream, DataInputStream, InputStream}
import javax.imageio.ImageIO
import javax.imageio.stream.ImageInputStream
import javax.xml.parsers.SAXParser
import scala.util.{Failure, Try}
import scala.xml.Elem
import scala.xml.factory.XMLLoader

trait ImageServlet {
  this: KoutaServlet =>

  def s3ImageService: S3ImageService

  def storeTempImage(sizeSpecs: ImageSizeSpecs)(implicit authenticated: Authenticated): String = {
    val image = readImage(sizeSpecs)

    image.format match {
      case ImageFormat.Svg =>
        checkSvg(image.data)
      case _ =>
        checkImageFormatAndSize(image, sizeSpecs)
    }

    s3ImageService.storeTempImage(image)
  }

  private def readImage(sizeSpecs: ImageSizeSpecs): Image = {
    val length = checkLength(sizeSpecs.maxSize)
    val imageFormat = checkContentType()
    val imageData = readImageDataFromStream(request.inputStream, length)
    Image(imageFormat, imageData)
  }

  private def checkSvg(imageData: Array[Byte]): Unit = {
    Try[Boolean] {
      val xml = DtdIgnoringXML.load(new ByteArrayInputStream(imageData))
      xml.label == "svg"
    }.toOption.filter(identity).getOrElse {
      throw MediaNotSupportedException(s"Tiedostoa ei voitu lukea image/svg+xml -kuvana")
    }
  }

  private object DtdIgnoringXML extends XMLLoader[Elem] {
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

  private def checkContentType(): ImageFormat = request.contentType.map(ImageFormat.get) match {
    case Some(Some(t)) => t
    case None => throw new IllegalArgumentException("Otsake Content-Type puuttuu")
    case Some(None) =>
      throw MediaNotSupportedException(
        s"Kuvamuoto ${request.contentType} ei ole tuettu. Sallitut muodot ovat: ${ImageFormat.allowedFormats.mkString(", ")}"
      )
  }

  private def readImageDataFromStream(inputStream: InputStream, length: Int): Array[Byte] = {
    val dataArray = new Array[Byte](length)
    val dis = new DataInputStream(inputStream)

    val imageData = Try {
      dis.readFully(dataArray, 0, length)
      dataArray
    }
    dis.close()
    imageData.get
  }

  private def checkImageFormat(image: Image) = {
    ImageIO.setUseCache(false)
    val mimeReaders = ImageIO.getImageReadersByMIMEType(image.format.contentType)
    val reader = mimeReaders.next()
    val bufferedImage = Try {
      reader.setInput(ImageIO.createImageInputStream(new ByteArrayInputStream(image.data)))
      reader.read(reader.getMinIndex)
    }.recoverWith {
      case e: Exception =>
        logger.warn("Exception while reading image", e)
        Failure(MediaNotSupportedException(s"Tiedostoa ei voitu lukea ${image.format.contentType} -kuvana"))
    }

    reader.getInput match {
      case stream: ImageInputStream => stream.close()
      case _ =>
    }
    reader.dispose()

    bufferedImage
  }

  private def checkImageFormatAndSize(image: Image, sizeSpecs: ImageSizeSpecs): Unit = {
    val bufferedImage = checkImageFormat(image)

    val (width, height) = (bufferedImage.get.getWidth, bufferedImage.get.getHeight)

    def erroMessagePrefix(width: Int, height: Int) = {
      s"Kuva on väärän kokoinen ($width x $height)"
    }

    def tooSmallImageMessage(specs: ImageSizeSpecs) = {
      s"minimikoko on ${specs.minWidth} x ${specs.minHeight}"
    }

    def tooLargeImageMessage(specs: ImageWithMaxSizeSpecs) =
      s"maksimikoko on ${specs.maxWidth} x ${specs.maxHeight}"

    sizeSpecs match {
      case specs: ImageWithMinSizeSpecs => {
        if (width < specs.minWidth || height < specs.minHeight) {
          throw new IllegalArgumentException(
            s"${erroMessagePrefix(width, height)}, kun ${tooSmallImageMessage(specs)}"
          )
        }
      }
      case specs: ImageWithMaxSizeSpecs =>
        val tooLarge = if (width > specs.maxWidth || height > specs.maxHeight) Some(tooLargeImageMessage(specs)) else None
        val tooSmall = if (width < specs.minWidth || height < specs.minHeight) Some(tooSmallImageMessage(specs)) else None
        if (tooLarge.isDefined || tooSmall.isDefined) {
          val errorMessage = tooLarge match {
            case Some(largeMessage) => largeMessage
            case None => tooSmall match {
              case Some(smallMessage) => smallMessage
            }
          }

          throw new IllegalArgumentException(
            s"${erroMessagePrefix(width, height)}, kun $errorMessage"
          )
        }
    }

  }
}
