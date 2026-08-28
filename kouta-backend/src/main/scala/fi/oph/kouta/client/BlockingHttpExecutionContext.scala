package fi.oph.kouta.client

import fi.oph.kouta.logging.Logging

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{SynchronousQueue, ThreadFactory, ThreadPoolExecutor, TimeUnit}
import scala.concurrent.ExecutionContext

/** Oma ExecutionContext estäville HTTP-kutsuille.
 *
 * Näitä ei saa ajaa `scala.concurrent.ExecutionContext.Implicits.globalissa`, koska sama pooli ajaa myös Slickin
 * DBIO-jatkumot (DAO:iden for-komprehensiot). Globaalin ForkJoinPoolin rinnakkaisuus on `availableProcessors`, eli
 * kontissa tyypillisesti 2-4 säiettä. Muutama hidas upstream-kutsu täyttää sen, jolloin monivaiheisen DBIO:n
 * jatkumo ei saa säiettä, kesken oleva transaktio pitää Hikari-yhteytensä odottaessaan, yhteyspooli tyhjenee ja
 * kaikki kantaoperaatiot alkavat aikakatkaista.
 *
 * Pooli ei jonota (SynchronousQueue): tehtävä saa säikeen heti tai se ajetaan kutsujan säikeessä
 * (CallerRunsPolicy). Kutsuja odottaa tulosta Awaitilla joka tapauksessa, joten jonottaminen vain siirtäisi saman
 * nälkiintymisen tänne. Säikeiden määrää rajaa käytännössä Jettyn säiepooli, ja joutilaat säikeet kuolevat pois
 * keepAlive-ajan jälkeen.
 */
object BlockingHttpExecutionContext extends Logging {
  private val MaxThreads       = 256
  private val KeepAliveSeconds = 60L

  private val threadFactory = new ThreadFactory {
    private val counter = new AtomicInteger(0)

    override def newThread(r: Runnable): Thread = {
      val thread = new Thread(r, s"kouta-http-${counter.incrementAndGet()}")
      thread.setDaemon(true)
      thread
    }
  }

  private val executor = new ThreadPoolExecutor(
    0,
    MaxThreads,
    KeepAliveSeconds,
    TimeUnit.SECONDS,
    new SynchronousQueue[Runnable](),
    threadFactory,
    new ThreadPoolExecutor.CallerRunsPolicy()
  )

  implicit val blockingHttpEc: ExecutionContext =
    ExecutionContext.fromExecutor(executor, t => logger.error("Käsittelemätön virhe estävässä HTTP-kutsussa", t))
}