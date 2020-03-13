package fi.oph.kouta.images

abstract class ImageFormat(val contentType: String, val extension: String) extends Product with Serializable

object ImageFormat {
  case object Jpeg extends ImageFormat("image/jpeg", "jpg")
  case object Png  extends ImageFormat("image/png", "png")
  case object Svg  extends ImageFormat("image/svg+xml", "svg")

  val allowedFormats = Set(Jpeg, Png, Svg)
  val typeMap: Map[String, ImageFormat] = allowedFormats.map(f => f.contentType -> f).toMap

  def get(contentType: String): Option[ImageFormat] = typeMap.get(contentType)
}

case class Image(format: ImageFormat, data: Array[Byte])
