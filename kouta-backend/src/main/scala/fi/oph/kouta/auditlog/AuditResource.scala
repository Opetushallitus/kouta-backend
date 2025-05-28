package fi.oph.kouta.auditlog

import fi.oph.kouta.domain
import fi.oph.kouta.domain.keyword

abstract class AuditResource(val name: String, val idField: String) extends AuditResourceOperations {
  override def toString: String = name
}

object AuditResource {
  case object Koulutus         extends AuditResource("koulutus", "oid")
  case object Toteutus         extends AuditResource("toteutus", "oid")
  case object Haku             extends AuditResource("haku", "oid")
  case object Hakukohde        extends AuditResource("hakukohde", "oid")
  case object Valintaperuste   extends AuditResource("valintaperuste", "id")
  case object Sorakuvaus       extends AuditResource("sorakuvaus", "id")
  case object Oppilaitos       extends AuditResource("oppilaitos", "oid")
  case object OppilaitoksenOsa extends AuditResource("oppilaitoksen_osa", "oid")
  case object Asiasana         extends AuditResource("asiasana", "asiasana")
  case object Ammattinimike    extends AuditResource("ammattinimike", "ammattinimike")
  case object Luokittelutermi  extends AuditResource("luokittelutermi", "luokittelutermi")

  def apply(entity: AnyRef): AuditResource = {
    entity match {
      case _: domain.Koulutus         => Koulutus
      case _: domain.Toteutus         => Toteutus
      case _: domain.Haku             => Haku
      case _: domain.Hakukohde        => Hakukohde
      case _: domain.Valintaperuste   => Valintaperuste
      case _: domain.Sorakuvaus       => Sorakuvaus
      case _: domain.Oppilaitos       => Oppilaitos
      case _: domain.OppilaitoksenOsa => OppilaitoksenOsa
      case keyword.Ammattinimike      => Ammattinimike
      case keyword.Asiasana           => Asiasana
      case keyword.Luokittelutermi    => Luokittelutermi
      case r: AuditResource           => r
      case _                          => throw new IllegalArgumentException("Resource not found")
    }
  }
}
