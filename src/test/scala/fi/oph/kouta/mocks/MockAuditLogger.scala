package fi.oph.kouta.mocks

import fi.vm.sade.auditlog.Logger

object MockAuditLogger extends Logger {
  val logs = scala.collection.mutable.ArrayBuffer[String]()

  private var debugPrint = false

  def log(log: String): Unit = {
    if (debugPrint) println(s"MOCKLOG: $log")
    logs += log
  }

  def clean(debug: Boolean = false): Unit = {
    debugPrint = debug
    logs.clear()
  }

  def find(first: String, searchStrings: String*): Option[String] = {
    debugPrint = false
    logs.find(log => (first +: searchStrings).forall(log.contains))
  }

  def findFieldChange(fieldName: String, oldValue: String, newValue: String, others: String*): Option[String] = {
    val change = s"""{"fieldName":"$fieldName","oldValue":"$oldValue","newValue":"$newValue"}"""
    find(change, others: _*)
  }
}
