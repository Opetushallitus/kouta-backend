package fi.oph.kouta

//TODO: Nämä kuuluu scala-utilsiin - pitää poistaa, kun scala-utils on saatavana scala 2.12:lle

class ChooseFreePort extends PortChooser {
  lazy val chosenPort = PortChecker.findFreeLocalPort
}

import java.io.IOException
import java.net.Socket

import scala.util.Random

object PortChecker {
  def isFreeLocalPort(port: Int): Boolean = {
    try {
      val socket = new Socket("127.0.0.1", port)
      socket.close()
      false
    } catch {
      case e:IOException => true
    }
  }

  def findFreeLocalPort: Int = {
    val range = 1024 to 60000
    val port = ((range(new Random().nextInt(range length))))
    if (isFreeLocalPort(port)) {
      port
    } else {
      findFreeLocalPort
    }
  }
}

trait PortChooser {
  val chosenPort: Int
}

class PortFromSystemPropertyOrFindFree(systemPropertyName: String) extends PortChooser {
  lazy val chosenPort = System.getProperty(systemPropertyName, PortChecker.findFreeLocalPort.toString).toInt
}

