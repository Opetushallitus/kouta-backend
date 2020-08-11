package fi.oph.kouta

import fi.oph.kouta.domain.Koulutustyyppi
import fi.oph.kouta.domain.oid.OrganisaatioOid

package object client {
  type OrganisaatioOidsAndOppilaitostyypitFlat = (Seq[OrganisaatioOid], Seq[Koulutustyyppi])
}
