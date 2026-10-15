package fi.oph.kouta.service.yos

import fi.oph.kouta.service.yos.YosKoulutusAsteLuokka.YosKoulutusAsteLuokka

import java.time.LocalDateTime

case class YosHakukohde(
                         korkeakoulutus: Boolean,
                         tutkintoonJohtava: Boolean,
                         jatkoTutkinto: Boolean,
                         kaksoisTutkinto: Boolean,
                         organisaatioJaVanhemmat: List[String],
                         koulutusAste: YosKoulutusAsteLuokka,
                         haunAlkamisaika: Option[LocalDateTime],
                         koulutuksenAlkamisvuosi: Option[String],
                       )
