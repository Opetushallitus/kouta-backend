package fi.oph.kouta.util

import java.nio.file.{Files, Paths}

object ConfigUtils {
  def getModuleDir(moduleName: String): String = {
    val userDir = System.getProperty("user.dir")
    if (Files.exists(Paths.get(userDir, "src"))) {
      userDir
    } else {
      Paths.get(userDir, moduleName).toString
    }
  }
}
