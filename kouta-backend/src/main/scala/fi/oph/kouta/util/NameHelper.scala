package fi.oph.kouta.util

import fi.oph.kouta.domain.{
  En,
  Fi,
  Kieli,
  Kielistetty,
  KoulutusMetadata,
  LukioKoulutusMetadata,
  LukioToteutusMetadata,
  LukiolinjaTieto,
  Sv,
  Toteutus,
  ToteutusMetadata
}
import fi.oph.kouta.client.Henkilo

object NameHelper {
  val EmptyKielistetty: Kielistetty = Map(
    Fi -> "",
    Sv -> "",
    En -> ""
  )
  def localizedKielistetty(kielistetty: Option[Kielistetty], kieli: Kieli): String = {
    kielistetty.getOrElse(EmptyKielistetty).getOrElse(kieli, "")
  }
  def isEmptyKielistetty(kielistetty: Option[Kielistetty]): Boolean =
    kielistetty.isDefined && kielistetty.get.forall { case (lng, translation) => translation.isEmpty }
  def generateLaajuus(laajuus: Option[String], opintopistetta: Option[Kielistetty], lng: Kieli): String = {
    if (laajuus.nonEmpty && !isEmptyKielistetty(opintopistetta)) {
      laajuus.get + " " + localizedKielistetty(opintopistetta, lng)
    } else {
      ""
    }
  }
  def getLukiolinjat(toteutus: Toteutus): Option[Seq[LukiolinjaTieto]] = {
    toteutus.metadata match {
      case Some(toteutusMetadata) =>
        toteutusMetadata match {
          case lukio: LukioToteutusMetadata => Some(lukio.painotukset ++ lukio.erityisetKoulutustehtavat)
          case _                            => None
        }
      case _ => None
    }
  }

  def getLukioKoulutusLaajuusNumero(lukioKoulutusMetadata: LukioKoulutusMetadata): Option[String] = {
    lukioKoulutusMetadata.opintojenLaajuusNumero.map(_.toInt.toString)
  }

  def generateLukioToteutusDisplayName(
      toteutusMetadata: LukioToteutusMetadata,
      koulutusMetadata: LukioKoulutusMetadata,
      kaannokset: Map[String, Kielistetty],
      koodiKaannokset: Map[String, Kielistetty]
  ): Kielistetty = {
    val yleislinjaNimiOsa = kaannokset.get("toteutuslomake.lukionYleislinjaNimiOsa")
    val opintopistetta    = kaannokset.get("yleiset.opintopistetta")
    val lukiolinjat       = toteutusMetadata.painotukset ++ toteutusMetadata.erityisetKoulutustehtavat

    val hasYleislinja = toteutusMetadata.yleislinja

    val lukiolinjaKaannokset = (if (hasYleislinja && yleislinjaNimiOsa.nonEmpty) Seq(yleislinjaNimiOsa) else Seq()) ++
      lukiolinjat.map(l => koodiKaannokset.get(l.koodiUri.split("#").head))

    val laajuusNumero = getLukioKoulutusLaajuusNumero(koulutusMetadata)

    List(Fi, Sv, En)
      .map(lng =>
        (
          lng,
          if (lukiolinjaKaannokset.forall(_.exists(k => k.contains(lng)))) {
            lukiolinjaKaannokset
              .map(linjaKaannos => {
                val linjaTranslation = localizedKielistetty(linjaKaannos, lng)
                if (linjaTranslation.nonEmpty)
                  linjaTranslation + ", " + generateLaajuus(laajuusNumero, opintopistetta, lng)
                else ""
              })
              .mkString("\n")
              .trim
          } else ""
        )
      )
      .filter { case (_, kaannos) => kaannos.nonEmpty }
      .toMap
  }

  def generateHakukohdeDisplayNameForTuva(
      hakukohdeNimi: Kielistetty,
      toteutusMetadata: ToteutusMetadata,
      kaannokset: Kielistetty
  ): Kielistetty = {
    val jarjestetaanErityisopetuksena = HakukohdeServiceUtil.getJarjestetaanErityisopetuksena(toteutusMetadata)

    if (jarjestetaanErityisopetuksena) {
      val defaultKaannos = kaannokset.get(Fi).getOrElse("")
      hakukohdeNimi.map { case (key, value) =>
        val kaannos = kaannokset.get(key)
        kaannos match {
          case Some(str) => key -> (value + s" ($str)")
          case None =>
            if (defaultKaannos.isEmpty) {
              key -> value
            } else {
              key -> (value + s" ($defaultKaannos)")
            }
        }
      }
    } else {
      hakukohdeNimi
    }
  }

  def generateMuokkaajanNimi(henkilo: Henkilo): String = {
    val kutsumanimi = henkilo.kutsumanimi.getOrElse("")
    val etunimet    = henkilo.etunimet.getOrElse("")
    val lastname    = henkilo.sukunimi.getOrElse("")

    val firstname = if (kutsumanimi.nonEmpty) kutsumanimi else etunimet
    s"${firstname} ${lastname}".trim()
  }

  def kielistettyWoNullValues(kielistetty: Kielistetty): Kielistetty =
    kielistetty.filter({case (_, valueStr) => valueStr != null})

  def mergeNames(source: Kielistetty, target: Kielistetty, kielivalinta: Seq[Kieli]): Kielistetty = {
    val targetWoNulls = kielistettyWoNullValues(target)
    kielistettyWoNullValues(source)
      .filterKeys(kielivalinta.contains(_))
      .map({ case (kieli, nameItem) =>
        if (targetWoNulls.getOrElse(kieli, "").isEmpty) (kieli, nameItem) else (kieli, target(kieli))
      })
  }

  def notFullyPopulated(nimi: Kielistetty, kielivalinta: Seq[Kieli]): Boolean = {
    val nimiWoNullValues = kielistettyWoNullValues(nimi)
    kielivalinta.exists(nimiWoNullValues.getOrElse(_, "").isEmpty)
  }
}
