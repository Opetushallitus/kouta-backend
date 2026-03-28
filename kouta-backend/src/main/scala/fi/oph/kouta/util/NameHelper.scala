package fi.oph.kouta.util

import fi.oph.kouta.domain.{
  En,
  Fi,
  Kieli,
  Kielistetty,
  LukioKoulutusMetadata,
  LukioToteutusMetadata,
  Sv,
  ToteutusEnrichmentSourceData,
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

  def generateLukioToteutusDisplayName(
      source: ToteutusEnrichmentSourceData,
      kaannokset: Map[String, Kielistetty],
      koodiKaannokset: Map[String, Kielistetty]
  ): Kielistetty = {
    val yleislinjaNimiOsa = kaannokset.get("toteutuslomake.lukionYleislinjaNimiOsa")
    val opintopistetta    = kaannokset.get("yleiset.opintopistetta")

    val lukiolinjaKaannokset =
      (if (source.hasLukioYleislinja && yleislinjaNimiOsa.nonEmpty) Seq(yleislinjaNimiOsa) else Seq()) ++
        source.lukioLinjat.map(l => koodiKaannokset.get(l.koodiUri.split("#").head))

    val laajuusNumero = source.opintojenLaajuusNumero.map(_.toInt.toString)

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
    kielistetty.filter({ case (_, valueStr) => valueStr != null })

  def mergeNames(source: Kielistetty, target: Kielistetty, kielivalinta: Seq[Kieli]): Kielistetty = {
    val targetWoNulls = kielistettyWoNullValues(target)
    kielistettyWoNullValues(source)
      .view.filterKeys(kielivalinta.contains(_))
      .map({ case (kieli, nameItem) =>
        if (targetWoNulls.getOrElse(kieli, "").isEmpty) (kieli, nameItem) else (kieli, target(kieli))
      }).toMap
  }

  def concatAsEntityName(start: Kielistetty, separator: Option[String], end: Kielistetty = Map(), kielivalinta: Seq[Kieli] = List(Fi)): Kielistetty = {
    def concatSeparatorToEnd(separator: Option[String], kaannos: (Kieli, String)): String = {
      s"${separator.getOrElse("")} ${kaannos._2}"
    }

    kielivalinta.map(kieli => {
      val startPart = start.find(kielistettyStart => kielistettyStart._1 == kieli) match {
        case Some(kaannos) => kaannos._2
        case None => start.find(kielistettyStart => kielistettyStart._1 == Fi).map(_._2).getOrElse("")
      }

      val endPart = end.find(kielistettyEnd => kielistettyEnd._1 == kieli)
      val endPartWithPossibleSeparator = endPart match {
        case Some(endPartWithRightLang) => concatSeparatorToEnd(separator, endPartWithRightLang)
        case None => end.find(kielistettyEnd => kielistettyEnd._1 == Fi) match {
          case Some(endPartWithFi) => concatSeparatorToEnd(separator, endPartWithFi)
          case None => ""
        }
      }
      kieli -> (startPart + endPartWithPossibleSeparator)
    }).toMap
  }

  def notFullyPopulated(nimi: Kielistetty, kielivalinta: Seq[Kieli]): Boolean = {
    val nimiWoNullValues = kielistettyWoNullValues(nimi)
    kielivalinta.exists(nimiWoNullValues.getOrElse(_, "").isEmpty)
  }
}
