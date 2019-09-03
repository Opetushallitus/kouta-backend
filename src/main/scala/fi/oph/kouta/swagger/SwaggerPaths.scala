package fi.oph.kouta.swagger

object SwaggerPaths {

  var paths: Map[String, List[String]] = Map[String, List[String]]()

  def registerPath(path: String, yaml: String): Unit =
    paths += (path -> (paths.getOrElse(path, List[String]()) ++ List(yaml)))

}
