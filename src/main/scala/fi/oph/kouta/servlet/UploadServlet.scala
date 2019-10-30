package fi.oph.kouta.servlet

import java.io._
import java.util.UUID

import fi.oph.kouta.SwaggerPaths.registerPath
import fi.oph.kouta.indexing.S3Service
import javax.imageio.ImageIO
import org.scalatra.Ok
import org.scalatra.servlet.SizeConstraintExceededException

class UploadServlet(s3Service: S3Service) extends KoutaServlet {

  def this() = this(S3Service)

  val allowedImageTypes: Map[String, String] = Map("image/jpeg" -> "jpg", "image/png" -> "png")
  val maxSize: Int = 2 * 1024 * 1024

  registerPath("/upload/image",
    s"""    post:
       |      summary: Tallenna koulutukselle teemakuva
       |      operationId: Tallenna koulutukselle teemakuva
       |      description: Tallenna koulutukselle teemakuva.
       |        Teemakuva korvaa mahdollisen aikaisemman kuvan.
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
       |""".stripMargin)
  post("/image") {

    val contentType = request.contentType
    logger.error(s"Content-Type: $contentType")

    val extension = contentType.flatMap(allowedImageTypes.get)
      .getOrElse(throw new IllegalArgumentException(s"Content-Type missing or invalid. Allowed types are ${allowedImageTypes.keys.mkString(", ")}"))
    
    val length = request.contentLength.get.toInt
    logger.error(s"$length")

    if (length == 0) {
      throw new IllegalArgumentException(s"Invalid Content-Length")
    }
    if (length > maxSize) {
      throw new SizeConstraintExceededException(s"Maximum size is $maxSize bytes", null)
    }

    val key = s"temp/${UUID.randomUUID}.$extension"
    logger.error(s"key $key")

    logger.error(s"$contextPath${request.getServletPath}$requestPath${request.queryString}")

    val array = new Array[Byte](length)
    val dis = new DataInputStream(request.inputStream)
    dis.readFully(array, 0, length)
    dis.close()

    logger.error(s"${array.length}")

    val image = ImageIO.read(new ByteArrayInputStream(array))
    val height = image.getHeight
    val width = image.getWidth

    logger.error(s"$width x $height")

    Ok("result" -> S3Service.storeImage(key, contentType.get, array))
  }

}

case class PayloadTooLargeException(message: String) extends RuntimeException(message)

case class MediaNotSupportedException(message: String) extends RuntimeException(message)
