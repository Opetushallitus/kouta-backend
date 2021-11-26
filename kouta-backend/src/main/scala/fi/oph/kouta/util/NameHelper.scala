package fi.oph.kouta.util

import fi.oph.kouta.domain.{
  En,
  Fi,
  Kieli,
  Kielistetty,
  Koulutus,
  LukioKoulutusMetadata,
  LukioToteutusMetadata,
  LukiolinjaTieto,
  Sv,
  Toteutus,
  ToteutusMetadata
}

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
        }
    }
  }
  def generateLukioToteutusDisplayName(
      toteutus: Toteutus,
      koulutus: Koulutus,
      kaannokset: Map[String, Kielistetty],
      koodiKaannokset: Map[String, Kielistetty]
  ): Kielistetty = {
    val yleislinjaNimiOsa = kaannokset.get("toteutuslomake.lukionYleislinjaNimiOsa")
    val opintopistetta    = kaannokset.get("yleiset.opintopistetta")
    val lukiolinjat       = (for {
      m <- toteutus.metadata.asInstanceOf[Option[LukioToteutusMetadata]]
    } yield m.painotukset ++ m.erityisetKoulutustehtavat).getOrElse(Seq())

    val hasYleislinja = (for {
      m <- toteutus.metadata.asInstanceOf[Option[LukioToteutusMetadata]]
    } yield m.yleislinja).getOrElse(false)

    val lukiolinjaKaannokset = (if (hasYleislinja && yleislinjaNimiOsa.nonEmpty) Seq(yleislinjaNimiOsa) else Seq()) ++
      lukiolinjat.map(l => koodiKaannokset.get(l.koodiUri.split("#").head))

    val laajuusNumero = for {
      m <- koulutus.metadata
      l <- m.asInstanceOf[LukioKoulutusMetadata].opintojenLaajuusKoodiUri
    } yield l.split("#").head.split('_').last

    List(Fi, Sv, En)
      .map(lng =>
        (
          lng,
          if (lukiolinjaKaannokset.forall(kaannos => kaannos.getOrElse(Map()).get(lng).nonEmpty)) {
            lukiolinjaKaannokset
              .map(linjaKaannos => {
                val linjaTranslation = localizedKielistetty(linjaKaannos, lng)
                if (linjaTranslation.nonEmpty)
                  linjaTranslation + ", " + generateLaajuus(laajuusNumero, opintopistetta, lng)
                else ""
              })
              .mkString("\n")
              .trim
          }
          else ""
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
          case None => if (defaultKaannos.isEmpty) {
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
}
