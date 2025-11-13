package fi.oph.kouta.properties

import java.util
import scala.collection.Map

/**
 * Extends OphProperties with scala types
 */
class OphProperties(files: String*) extends fi.vm.sade.properties.OphProperties(files:_*) {
  private val excludeCCFields = List("$outer")
  private def caseClassToMap(cc: Product) = {
    val declaredFields = cc.getClass.getDeclaredFields.toList.filter( f => !excludeCCFields.contains(f.getName))
    (Map[AnyRef, AnyRef]() /: declaredFields) {(a, f) =>
      f.setAccessible(true)
      a + (f.getName -> f.get(cc))
    }
  }

  private def removeOption(map: Map[AnyRef, AnyRef]) = {
    for( (k,v) <- map if v != None)
      yield (k, v match {
        case Some(option:AnyRef) => option
        case _ => v
      } )
  }

  private def toJavaMap(map: Map[AnyRef, AnyRef]) = {
    val dest = new util.LinkedHashMap[AnyRef, AnyRef](map.size)
    val option: Map[AnyRef, AnyRef] = removeOption(map)
    option.foreach{ case (k,v) => dest.put(k,convertToJava(v))}
    dest
  }

  private def toJavaList(seq: Seq[AnyRef]) = {
    val dest = new util.ArrayList[AnyRef](seq.size)
    seq.foreach{ case (u) => dest.add(convertToJava(u))}
    dest
  }

  private def convertToJava(o:AnyRef): AnyRef = o match {
    case seq: Seq[AnyRef] =>
      toJavaList(seq.asInstanceOf[Seq[AnyRef]])
    case map: Map[AnyRef, AnyRef] =>
      toJavaMap(map.asInstanceOf[Map[AnyRef, AnyRef]])
    case cc: Product =>
      toJavaMap(caseClassToMap(cc))
    case _ =>
      o
  }

  override def convertParams(params: AnyRef*): Array[AnyRef] = {
    params.map(convertToJava).toArray
  }
}
