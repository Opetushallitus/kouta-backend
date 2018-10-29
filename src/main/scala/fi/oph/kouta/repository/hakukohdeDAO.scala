package fi.oph.kouta.repository

import java.sql.Timestamp
import java.time.{Instant, ZoneId}
import java.time.format.DateTimeFormatter
import java.util.ConcurrentModificationException

import fi.oph.kouta.domain
import fi.oph.kouta.domain.Hakukohde
import slick.dbio.DBIO
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global

trait HakukohdeDAO {
  def put(hakukohde:Hakukohde):Option[String]
  def get(oid:String): Option[(Hakukohde, Instant)]
  def getLastModified(oid:String): Option[Instant]
  def update(haku:Hakukohde, notModifiedSince:Instant): Boolean
}

object HakukohdeDAO extends HakukohdeDAO with HakukohdeSQL {

  override def put(hakukohde: Hakukohde): Option[String] = {
    KoutaDatabase.runBlockingTransactionally( for {
      oid <- insertHakukohde(hakukohde)
      _ <- insertHakuajat(hakukohde.copy(oid = oid))
    } yield (oid) ) match {
      case Left(t) => throw t
      case Right(oid) => oid
    }
  }

  override def get(oid: String): Option[(Hakukohde, Instant)] = {
    KoutaDatabase.runBlockingTransactionally( for {
      h <- selectHakukohde(oid)
      a <- selectHaunHakuajat(oid)
      l <- selectLastModified(oid)
    } yield (h, a, l) ) match {
      case Left(t) => throw t
      case Right((None, _, _)) | Right((_, _, None)) => None
      case Right((Some(h), a, Some(l))) => Some((h.copy(hakuajat = a.map(x => domain.Hakuaika(x.alkaa, x.paattyy)).toList), l))
    }
  }

  override def getLastModified(oid: String): Option[Instant] = KoutaDatabase.runBlocking( selectLastModified(oid) )

  override def update(hakukohde: Hakukohde, notModifiedSince: Instant): Boolean = {
    KoutaDatabase.runBlockingTransactionally( selectLastModified(hakukohde.oid.get).flatMap(_ match {
      case None => DBIO.failed(new NoSuchElementException(s"Unknown hakukohde oid ${hakukohde.oid.get}"))
      case Some(time) if time.isAfter(notModifiedSince) => DBIO.failed(new ConcurrentModificationException(s"Joku oli muokannut hakukohdetta ${hakukohde.oid.get} samanaikaisesti"))
      case Some(time) => DBIO.successful(time)
    }).andThen(updateHakukohde(hakukohde))
      .zip(updateHakuajat(hakukohde))) match {
      case Left(t) => throw t
      case Right((x, y)) => 0 < (x + y.sum)
    }
  }
}

sealed trait HakukohdeSQL extends SQLHelpers with HakukohdeExctractors {

  def selectLastModified(oid:String):DBIO[Option[Instant]] = {
    sql"""select greatest(
            max(lower(ha.system_time)),
            max(lower(hah.system_time)),
            max(upper(hh.system_time)),
            max(upper(hhh.system_time)))
          from hakukohteet ha
          left join hakukohteet_history hah on ha.oid = hah.oid
          left join hakukohteiden_hakuajat hh on ha.oid = hh.hakukohde_oid
          left join hakukohteiden_hakuajat_history hhh on ha.oid = hhh.hakukohde_oid
          where ha.oid = $oid""".as[Option[Instant]].head
  }

  def insertHakukohde(hakukohde:Hakukohde) = {
    sql"""insert into hakukohteet (
            koulutus_oid,
            haku_oid,
            tila,
            nimi,
            alkamiskausi_koodi_uri,
            alkamisvuosi,
            hakulomaketyyppi,
            hakulomake,
            aloituspaikat,
            ensikertalaisen_aloituspaikat,
            pohjakoulutusvaatimus_koodi_uri,
            muu_pohjakoulutusvaatimus_kuvaus,
            toinen_aste_onko_kaksoistutkinto,
            kaytetaan_haun_aikataulua,
            valintaperuste,
            metadata,
            muokkaaja,
            kielivalinta
          ) values (
            ${hakukohde.koulutusOid},
            ${hakukohde.hakuOid},
            ${hakukohde.tila.toString}::julkaisutila,
            ${toJsonParam(hakukohde.nimi)}::jsonb,
            ${hakukohde.alkamiskausiKoodiUri},
            ${hakukohde.alkamisvuosi},
            ${hakukohde.hakulomaketyyppi.map(_.toString)}::hakulomaketyyppi,
            ${hakukohde.hakulomake},
            ${hakukohde.aloituspaikat},
            ${hakukohde.ensikertalaisenAloituspaikat},
            ${hakukohde.pohjakoulutusvaatimusKoodiUri},
            ${toJsonParam(hakukohde.muuPohjakoulutusvaatimus)}::jsonb,
            ${hakukohde.toinenAsteOnkoKaksoistutkinto},
            ${hakukohde.kaytetaanHaunAikataulua},
            ${hakukohde.valintaperuste.map(_.toString)}::uuid,
            ${toJsonParam(hakukohde.metadata)}::jsonb,
            ${hakukohde.muokkaaja},
            ${toJsonParam(hakukohde.kielivalinta)}::jsonb
          ) returning oid""".as[String].headOption
  }

  def updateHakukohde(hakukohde:Hakukohde) = {
    sqlu"""update hakukohteet set
              koulutus_oid = ${hakukohde.koulutusOid},
              haku_oid = ${hakukohde.hakuOid},
              tila = ${hakukohde.tila.toString}::julkaisutila,
              nimi = ${toJsonParam(hakukohde.nimi)}::jsonb,
              alkamiskausi_koodi_uri = ${hakukohde.alkamiskausiKoodiUri},
              alkamisvuosi = ${hakukohde.alkamisvuosi},
              hakulomaketyyppi = ${hakukohde.hakulomaketyyppi.map(_.toString)}::hakulomaketyyppi,
              hakulomake = ${hakukohde.hakulomake},
              aloituspaikat = ${hakukohde.aloituspaikat},
              ensikertalaisen_aloituspaikat = ${hakukohde.ensikertalaisenAloituspaikat},
              pohjakoulutusvaatimus_koodi_uri = ${hakukohde.pohjakoulutusvaatimusKoodiUri},
              muu_pohjakoulutusvaatimus_kuvaus = ${toJsonParam(hakukohde.muuPohjakoulutusvaatimus)}::jsonb,
              toinen_aste_onko_kaksoistutkinto = ${hakukohde.toinenAsteOnkoKaksoistutkinto},
              kaytetaan_haun_aikataulua = ${hakukohde.kaytetaanHaunAikataulua},
              valintaperuste = ${hakukohde.valintaperuste.map(_.toString)}::uuid,
              metadata = ${toJsonParam(hakukohde.metadata)}::jsonb,
              muokkaaja = ${hakukohde.muokkaaja},
              kielivalinta = ${toJsonParam(hakukohde.kielivalinta)}::jsonb
          where oid = ${hakukohde.oid}
            and ( koulutus_oid <> ${hakukohde.koulutusOid}
            or haku_oid <> ${hakukohde.hakuOid}
            or tila <> ${hakukohde.tila.toString}::julkaisutila
            or nimi <> ${toJsonParam(hakukohde.nimi)}::jsonb
            or alkamiskausi_koodi_uri <> ${hakukohde.alkamiskausiKoodiUri}
            or alkamisvuosi <> ${hakukohde.alkamisvuosi}
            or hakulomaketyyppi <> ${hakukohde.hakulomaketyyppi.map(_.toString)}::hakulomaketyyppi
            or hakulomake <> ${hakukohde.hakulomake}
            or aloituspaikat <> ${hakukohde.aloituspaikat}
            or ensikertalaisen_aloituspaikat <> ${hakukohde.ensikertalaisenAloituspaikat}
            or pohjakoulutusvaatimus_koodi_uri <> ${hakukohde.pohjakoulutusvaatimusKoodiUri}
            or muu_pohjakoulutusvaatimus_kuvaus <> ${toJsonParam(hakukohde.muuPohjakoulutusvaatimus)}::jsonb
            or toinen_aste_onko_kaksoistutkinto <> ${hakukohde.toinenAsteOnkoKaksoistutkinto}
            or kaytetaan_haun_aikataulua <> ${hakukohde.kaytetaanHaunAikataulua}
            or valintaperuste <> ${hakukohde.valintaperuste.map(_.toString)}::uuid
            or metadata <> ${toJsonParam(hakukohde.metadata)}::jsonb
            or kielivalinta <> ${toJsonParam(hakukohde.kielivalinta)}::jsonb)"""
  }

  def selectHakukohde(oid:String) = {
    sql"""select oid,
             koulutus_oid,
             haku_oid,
             tila,
             nimi,
             alkamiskausi_koodi_uri,
             alkamisvuosi,
             hakulomaketyyppi,
             hakulomake,
             aloituspaikat,
             ensikertalaisen_aloituspaikat,
             pohjakoulutusvaatimus_koodi_uri,
             muu_pohjakoulutusvaatimus_kuvaus,
             toinen_aste_onko_kaksoistutkinto,
             kaytetaan_haun_aikataulua,
             valintaperuste,
             metadata,
             muokkaaja,
             kielivalinta from hakukohteet where oid = $oid""".as[Hakukohde].headOption
  }

  def insertHakuajat(hakukohde:Hakukohde) = {
    DBIO.sequence(
      hakukohde.hakuajat.map(t => (
        sqlu"""insert into hakukohteiden_hakuajat (hakukohde_oid, hakuaika, muokkaaja)
               values (${hakukohde.oid}, tstzrange(${Timestamp.from(t.alkaa)}, ${Timestamp.from(t.paattyy)}, '[)'), ${hakukohde.oid})""")))
  }

  def selectHaunHakuajat(oid:String) = {
    sql"""select hakukohde_oid, lower(hakuaika), upper(hakuaika) from hakukohteiden_hakuajat where hakukohde_oid = $oid""".as[Hakuaika]
  }

  def updateHakuajat(hakukohde:Hakukohde) = {
    val (oid, hakuajat, muokkaaja) = (hakukohde.oid, hakukohde.hakuajat, hakukohde.muokkaaja)
    if(hakuajat.size > 0) {
      val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.of("Europe/Helsinki"))
      val hakuajatString = hakuajat.map(s => s"'[${formatter.format(s.alkaa)}, ${formatter.format(s.paattyy)})'").mkString(",")
      DBIO.sequence( hakuajat.map(t => {
        sqlu"""insert into hakukohteiden_hakuajat (hakukohde_oid, hakuaika, muokkaaja)
               values ($oid, tstzrange(${Timestamp.from(t.alkaa)}, ${Timestamp.from(t.paattyy)}, '[)'), $muokkaaja)
               on conflict on constraint hakukohteiden_hakuajat_pkey do nothing"""
      }) :+ sqlu"""delete from hakukohteiden_hakuajat where hakukohde_oid = $oid and hakuaika not in (#${hakuajatString})""")
    } else {
      DBIO.sequence(List(sqlu"""delete from hakukohteiden_hakuajat where hakukohde_oid = $oid"""))
    }
  }
}