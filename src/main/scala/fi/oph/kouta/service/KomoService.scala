package fi.oph.kouta.service

import fi.oph.kouta.domain.Komo
import fi.oph.kouta.repository.KomoDAO

object KomoService {

    def put(komo:Komo) = KomoDAO.put(komo)

    def get(oid:String): Option[Komo] = KomoDAO.get(oid)
}
