package fi.oph.kouta.integration.fixture

import java.time.LocalDateTime
import java.util.UUID

import fi.oph.kouta.auditlog.AuditLog
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.integration.{AccessControlSpec, KoutaIntegrationSpec}
import fi.oph.kouta.mocks.MockAuditLogger
import fi.oph.kouta.repository.ValintaperusteDAO
import fi.oph.kouta.service.{OrganisaatioServiceImpl, ValintaperusteService}
import fi.oph.kouta.servlet.ValintaperusteServlet
import fi.oph.kouta.util.TimeUtils
import fi.oph.kouta.{SqsInTransactionServiceIgnoringIndexing, TestData}
import org.scalactic.Equality
import slick.jdbc.GetResult

trait ValintaperusteFixture extends KoutaIntegrationSpec with AccessControlSpec  {

  val ValintaperustePath = "/valintaperuste"

  def valintaperusteService: ValintaperusteService = {
    val organisaatioService = new OrganisaatioServiceImpl(urlProperties.get)
    new ValintaperusteService(SqsInTransactionServiceIgnoringIndexing, new AuditLog(MockAuditLogger), organisaatioService)
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    addServlet(new ValintaperusteServlet(valintaperusteService), ValintaperustePath)
  }

  val valintaperuste = TestData.AmmValintaperuste

  def getIds(valintaperuste: Valintaperuste): Valintaperuste = {
    import slick.jdbc.PostgresProfile.api._

    implicit val getUUIDResult: GetResult[UUID] = GetResult(r => {
      UUID.fromString(r.nextString())
    })

    valintaperuste.copy(
      valintakokeet = valintaperuste.valintakokeet.map(valintakoe => valintakoe.copy(id = db.runBlocking(
        sql"""select id from valintaperusteiden_valintakokeet
              where valintaperuste_id = ${valintaperuste.id.map(_.toString)}::uuid
                and tyyppi_koodi_uri = ${valintakoe.tyyppiKoodiUri}""".as[String]).headOption.map(UUID.fromString))),
      sorakuvausId = db.runBlocking(sql"select sorakuvaus_id from valintaperusteet where id = ${valintaperuste.id.map(_.toString)}::uuid".as[UUID].headOption)
    )
  }

  def tallennettuValintaperuste(id: UUID): Valintaperuste = getIds(valintaperuste.copy(id = Some(id)))

  def valintaperuste(sorakuvausId: UUID): Valintaperuste = valintaperuste.copy(sorakuvausId = Some(sorakuvausId))
  def valintaperuste(id:UUID, sorakuvausId: UUID): Valintaperuste = valintaperuste.copy(id = Some(id), sorakuvausId = Some(sorakuvausId))
  def valintaperuste(id:UUID, sorakuvausId: UUID, tila:Julkaisutila): Valintaperuste = valintaperuste.copy(id = Some(id), sorakuvausId = Some(sorakuvausId), tila = tila)

  def put(valintaperuste: Valintaperuste): UUID = put(ValintaperustePath, valintaperuste, id(_))
  def put(valintaperuste: Valintaperuste, sessionId: UUID): UUID = put(ValintaperustePath, valintaperuste, sessionId, id(_))

  implicit val valintaperusteEquality: Equality[Valintaperuste] = (a: Valintaperuste, b: Any) => b match {
    case v: Valintaperuste =>
      val that = a.copy(valintakokeet = a.valintakokeet.map(_.copy(id = None)).sortBy(_.nimi(Fi)))
      val other = v.copy(valintakokeet = v.valintakokeet.map(_.copy(id = None)).sortBy(_.nimi(Fi)))
      Equality.default[Valintaperuste].areEqual(that, other)
    case _ => false
  }

  def get(id: UUID, expected: Valintaperuste): String = get(ValintaperustePath, id, expected.copy(modified = Some(readValintaperusteModified(id))))
  def get(id: UUID, sessionId: UUID, expected: Valintaperuste): String = get(ValintaperustePath, id, sessionId, expected.copy(modified = Some(readValintaperusteModified(id))))

  def update(valintaperuste: Valintaperuste, lastModified: String, expectedStatus: Int, sessionId: UUID): Unit = update(ValintaperustePath, valintaperuste, lastModified, sessionId, expectedStatus)
  def update(valintaperuste: Valintaperuste, lastModified: String, expectUpdate: Boolean, sessionId: UUID): Unit = update(ValintaperustePath, valintaperuste, lastModified, expectUpdate, sessionId)
  def update(valintaperuste: Valintaperuste, lastModified: String, expectUpdate: Boolean): Unit = update(ValintaperustePath, valintaperuste, lastModified, expectUpdate)
  def update(valintaperuste: Valintaperuste, lastModified: String): Unit = update(valintaperuste, lastModified, true)

  def valintaperuste(sorakuvausId: Option[UUID], tila: Julkaisutila, organisaatioOid: OrganisaatioOid): Valintaperuste =
    valintaperuste.copy(sorakuvausId = sorakuvausId, organisaatioOid = organisaatioOid, tila = tila)

  def addToList(valintaperuste:Valintaperuste) = {
    val id = put(valintaperuste)
    val modified = readValintaperusteModified(id)
    ValintaperusteListItem(id, valintaperuste.nimi, valintaperuste.tila,
      valintaperuste.organisaatioOid, valintaperuste.muokkaaja, modified)
  }

  def readValintaperusteModified(id: String): LocalDateTime = readValintaperusteModified(UUID.fromString(id))
  def readValintaperusteModified(id: UUID): LocalDateTime =
    TimeUtils.instantToModifiedAt(db.runBlocking(ValintaperusteDAO.selectLastModified(id)).get)
}
