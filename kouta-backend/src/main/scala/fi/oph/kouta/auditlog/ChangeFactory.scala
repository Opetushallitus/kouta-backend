package fi.oph.kouta.auditlog

import cats.data.Chain
import cats.implicits._
import diffson.circe._
import diffson.diff
import diffson.jsonpatch._
import diffson.jsonpatch.lcsdiff.remembering.JsonDiffDiff
import diffson.jsonpointer.Part
import diffson.lcs.Patience
import fi.oph.kouta.util.KoutaJsonFormats
import fi.vm.sade.auditlog.Changes
import io.circe.Json
import io.circe.parser.parse

import scala.util.Try

private[auditlog] object ChangeFactory extends KoutaJsonFormats {

  implicit val lcs: Patience[Json] = new Patience[Json]

  def getChanges[T <: AnyRef](before: T, after: T): Changes = {
    val jsonBefore = parse(toJson(before)).toTry.get
    val jsonAfter = parse(toJson(after)).toTry.get

    val patch = diff(jsonBefore, jsonAfter)

    val changes = new Changes.Builder()
    patch.ops.foreach {
      case op: Remove[Json] =>
        val path = parseParts(op.path.parts)
        changes.removed(path, jsonString(op.old.get))
      case op: Replace[Json] =>
        val path = parseParts(op.path.parts)
        changes.updated(path, jsonString(op.old.get), jsonString(op.value))
      case op: Add[Json] =>
        val path = parseParts(op.path.parts)
        changes.added(path, jsonString(op.value))
      case op: Move[Json] =>
        val from = parseParts(op.from.parts)
        val to = parseParts(op.path.parts)
        val fromValue = op.from.evaluate[Try, Json](jsonBefore).get
        val toValue = op.from.evaluate[Try, Json](jsonAfter).get
        changes.removed(from, jsonString(fromValue))
        changes.added(to, jsonString(toValue))
      case op: Copy[Json] =>
        val to = parseParts(op.path.parts)
        val toValue = op.from.evaluate[Try, Json](jsonAfter).get
        changes.added(to, jsonString(toValue))
      case op =>
        throw new RuntimeException(s"Panicked about json patch ${op.getClass.getSimpleName}")
    }
    changes.build()
  }

  private def jsonString(json: Json): String = json.asString.getOrElse(json.noSpaces)

  private def parseParts(parts: Chain[Part]): String = parts.map(part => part.fold(identity, _.toString)).toList.mkString(".")
}
