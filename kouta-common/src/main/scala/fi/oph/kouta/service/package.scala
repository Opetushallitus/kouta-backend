package fi.oph.kouta

import fi.oph.kouta.client.OrganisaatioOidsAndOppilaitostyypitFlat

import scala.collection.IterableView

package object service {
  type OrganisaatioOidsAndOppilaitostyypitFlatView = IterableView[OrganisaatioOidsAndOppilaitostyypitFlat, Iterable[_]]
}
