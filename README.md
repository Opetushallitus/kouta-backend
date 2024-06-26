# Kouta-backend

[![Kouta-backend](https://github.com/Opetushallitus/kouta-backend/actions/workflows/build.yml/badge.svg)](https://github.com/Opetushallitus/kouta-backend/actions/workflows/build.yml)

Uuden koulutustarjonnan datan hallinnoija. Sisältää koulutustarjonnan master-datan, jota kouta-indeksoija rikastaa ja
jakelee muille palveluille.

## Arkkitehtuuri

Kouta-backend on Scalalla ja Scalatra frameworkilla toteutettu web api. Kouta-ui:n kautta syötetään koulutustarjonnan
tiedot, jotka kouta-backend validoi ja tallentaa. Tallennuksen yhteydessä lähetetään sqs-viesti jonoon jota
[kouta-indeksoija](https://github.com/Opetushallitus/kouta-indeksoija) kuuntelee. Viestissä on tallennetun entiteetin
id, jonka avulla [kouta-indeksoija](https://github.com/Opetushallitus/kouta-indeksoija) kysyy kouta-backendilta entiteetin tarkemmat
tiedot.

Kouta-backendin tarkoitus on olla uuden koulutustarjonnan tietomallin vartija. Tänne ei duplikoida muiden palvelujen dataa,
vaan muihin palveluihin viittaavasta datasta tallennetaan ainoastaan id (esim. eperusteId).
[Kouta-indeksoija](https://github.com/Opetushallitus/kouta-indeksoija) huolehtii näiden id-arvojen avulla tietojen
rikastamisesta esim. oppijan käyttäliittymää ([konfo-ui](https://github.com/Opetushallitus/konfo-ui)) varten.

Kuva koutan arkkitehtuurista löytyy [OPH:n wikistä.](https://wiki.eduuni.fi/display/OPHSS/Koutan+arkkitehtuuri)

### Tietokanta
Kouta-backendin data tallennetaan PostgreSQL-kantaan. Tietokantarakenteessa on ilmeisesti haluttu välttää vanhan tarjonnan
monimutkaisuus ja siksi skeema on haluttu pitää mahdollisimman flattina. Tästä johtuen skeema on sekoitus relaatiotietokantaa ja
dokumenttitietokantaa. Tämä on totetettu niin että entiteetin tärkeimmät kentät on taulun päätasolla ja loput kentät metadata nimisessä
kentässä jsonb-formaatissa olevassa tietorakenteessa.

Tietokantaan on määritelty jokaiselle taululle trigger funktio, joka laukeaa kun tauluun tulee uusi rivi tai olemassa olevaa päivitetään.
Funktio tallentaa vanhan arvon history tauluun (esim. koulutukset_history), jolloin entiteetin koko muutoshistoria on tallessa.

### Kouta-backendin käyttämät tietolähteet

| Tietolähde                                                   | Haettavat tiedot |
|--------------------------------------------------------------|------------------|
| käyttöoikeuspalvelu                                          | Tarkastetaan käyttäjän käyttöoikeudet |
| ohjausparametritpalvelu                                      | Asetetaan hakujen ohjausparametrit |
| organisaatiopalvelu                                          | Koulutustoimijoiden, oppilaitosten ja toimipisteiden organisaatiohierarkia |

### Rajapinta ulkoisille palveluille

Kouta-backendin external-rajapinta on tarkoitettu [kouta-external](https://github.com/Opetushallitus/kouta-external) palvelulle, jonka rajapintojen avulla koulutuksenjärjestäjät voivat 
päivittää koulutustarjontaansa rajapinnan kautta.

## Kehitysympäristö

### Esivaatimukset

Asenna haluamallasi tavalla koneellesi
1. [IntelliJ IDEA](https://www.jetbrains.com/idea/) + [scala plugin](https://plugins.jetbrains.com/plugin/1347-scala)
2. [AWS cli](https://aws.amazon.com/cli/) (SQS-jonoja varten)
3. [Docker](https://www.docker.com/get-started) (localstackia ja postgresia varten)
4. [Maven](https://maven.apache.org/) Jos haluat ajaa komentoriviltä Mavenia,
   mutta idean Mavenilla pärjää kyllä hyvin, joten tämä ei ole pakollinen

Lisäksi tarvitset Java SDK:n ja Scala SDK:n (Unix pohjaisissa käyttöjärjestelmissä auttaa esim. [SDKMAN!](https://sdkman.io/)). Katso [.github/workflows/build.yml](.github/workflows/build.yml) mitä versioita sovellus käyttää.
Kirjoitushetkellä käytössä Amazon Corretto 11 ja scala 2.12.10.

PostgreSQL Kontti-imagen luonti (tarvitsee tehdä vain kerran):

``` shell
cd kouta-backend/postgresql/docker
docker build --tag kouta-postgres .
```

### Kehitysympäristön ongelmat

Jos jostain syystä esivaatimusten jälkeen projektia avatessa koodi punoittaa paljon IDEA:ssa, eikä syntaksin väritys toimi, valitse IDEA:n project-ikkunassa
src/main-kansion alainen scala-kansio ja right click -> Mark directory as -> Sources root. Samoin src/test-kansion alainen scala-kansio, right click -> Mark directory as -> Test Sources root. Tee sama toimenpide sekä kouta-backendille että kouta-commonille.

### Testien ajaminen

Jos Maven on asennettuna voi testit ajaa komentoriviltä `mvn test` komennolla tai rajaamalla ajettavien testejä
`mvn test -Dsuites="<testiluokan nimet pilkulla erotettuna>"`

Testit voi ajaa ideassa Maven ikkunasta valitsemalla test kouta-backend-parentin kohdalta tai
avaamalla Edit Configurations valikon ja luomalla uuden Maven run configurationin jolle
laitetaan working directoryksi projektin juurikansio ja Command line komennoksi `test`. Tämän jälkeen konfiguraatio ajoon.

Yksittäisen testisuiten tai testin voi ajaa ottamalla right-click halutun testiclassin tai funktion päältä.

Testit käynnistävät PostgreSQL:n docker-kontissa satunnaiseen vapaaseen porttiin.

#### Debuggaus

Logitukseen käytetään Log4j-kirjastoa, jolle testeissä käytetään asetustiedostoa `kouta-backend/src/test/resources/log4j.properties`.
Debug-logituksen saa päälle vaihtamalla Log4j-rootLoggerille tason DEBUG:

```diff
- log4j.rootLogger=INFO, Console
+ log4j.rootLogger=DEBUG, Console
```

Jos haluat lisäksi tarkastella Slick-kirjaston muodostamia SQL-kyselyitä, voit myös lisätä samaan tiedostoon rivin:

- `logger.slick.session=DEBUG`

#### Testit ja Windows + docker

Jotkin testit (IndexerSpec) voivat epäonnistua windowsilla, koska getDockerExeLocation olettaa `docker.exe` löytyvän kovakoodatusti tietystä polusta:

- `C:/program files/docker/docker/resources/bin/docker.exe`

Tähän voi tehdä korjauksen ainakin seuraavalla tavalla: lisää ko. polkuun symbolinen linkki viittaamaan oikeaan polkuun, mistä docker.exe löytyy. Tämän saa helpoiten tehtyä windowsissa pikakuvakkeella.

### Migraatiot

Tietokantamigraatiot on toteutettu [flywaylla](https://flywaydb.org/) ja ajetaan automaattisesti testien ja asennuksen
yhteydessä. Migraatiotiedostot löytyvät kansiosta `kouta-backend/src/main/resources/db/migration`

### Ajaminen lokaalisti

Kopioi konfiguraatio-template lokaalia kehitystä varten ```'/src/test/resources/dev-vars.template.yml'``` -> ```'/src/test/resources/dev-vars.yml'```.
Dev-vars.yml on ignoroitu Gitissä ettei salasanat valu repoon.

Käynnistä Ideassa ```embeddedJettyLauncher.scala``` (right-click -> Run). Sovellus
käynnistyy porttiin **8099** ja käynnistää oletuksena PostgreSQL:n docker-kontissa.
Asetuksia voi muuttaa muokkaamalla ```'/src/test/resources/dev-vars.yml'```-tiedostoa.
HUOM! KayttooikeusClient ja OppijanumerorekisteriClient (joita käytetään muokkaajan nimen muodostamiseen) tarvitsevat toimiakseen testiympäristön CAS-salasanan, joka löytyy sieltä, mistä muutkin salasanat. Sovellus toimii kuitenkin ilman salasanan asettamista, mutta logeihin tulee siinä tapauksessa CasAuthenticationErroreita.

EmbeddedJettyLauncher luo automaattisesti myös SQS-jonot localstackiin porttiin localhost:4576.

Swagger löytyy osoitteesta [http://localhost:8099/kouta-backend/swagger/index.html](http://localhost:8099/kouta-backend/swagger/index.html).

Jos Docker ei ole ajossa EmbeddedJettyLauncher:n käynnistyksessä, niin saatat päätyä virheilmoitukseen
```com.amazonaws.SdkClientException: Unable to execute HTTP request: Read timed out```.
Tästä voi toipua ajamalla `tools/stop_localstack`, joka poistaa "haamutiedoston".

#### Indeksoinnin SQS-jonot

Indeksoinnin jonot saa manuaalisesti lokaalisti käyttöön porttiin `localhost:4576` käynnistämällä
Localstack Docker instanssin komennolla `tools/start_localstack` ja sammutettua
`tools/stop_localstack` komennolla.

Localstackin käynnistysskripti kirjoittaa `~/.kouta_localstack` -tiedostoon käynnistämänsä dockerin id:n.
Pysäytysskripti poistaa tuon tiedoston. Jos docker pysähtyy muulla tavalla, on mahdollista, että tuo tiedosto
jää paikoilleen, vaikka docker on jo sammunut. Silloin kannattaa ajaa `tools/stop_localstack`, joka poistaa "haamutiedoston".

### Ajo testiympäristöä vasten

Mikäli tulee tarve tutkia testiympäristön kantaa tai ajaa kouta-backendia jonkin testiympäristön kantaa vasten, yksi keino tähän on
SSH-porttiohjaus joka onnistuu seuraavalla komennolla (halutun ympäristön mukaan):

- ssh -N -L 5432:kouta.db.hahtuvaopintopolku.fi:5432 bastion.hahtuva

tai jos on ssh.configissa tarvittava porttiforwardointi, yksinkertaisemmin

- ssh bastion.hahtuva

Olettaen, että ssh-konfiguraatiot ovat pilviylläpidon ohjeistuksen mukaiset ja sieltä löytyy kuvatun mukainen Host-alias
ks. https://github.com/Opetushallitus/cloud-base/blob/master/tools/user-management/ssh.config.template
ja porttiforwardoinneille https://github.com/Opetushallitus/local-environment/blob/master/templates/ssh_config.template

Tämän lisäksi pitää vaihtaa `dev-vars.yml` tai `EmbeddedJettyLauncher.scala` tiedostoon postgresin salasana
vastaamaan testiympäristön kannan salasanaa. Salasanat löytyvät samasta paikasta kuin muutkin OPH:n palvelujen 
salaisuudet. Lisätietoja ylläpidolta.

Jos käytät muuta kuin templatessa oletuksena olevaa hahtuva-ympäristöä, vaihda ympäristön osoitteisiin viittaavat konfiguraatiot haluttuun ympristöön:
`host_virkailija`
`host_alb_virkailija`
`cas_url`
`kouta_backend_s3_image_bucket`
`kouta_backend_elasticsearch_url`

Lisäksi pitää vielä asettaa muutama VM-parametri EmbeddedJettyLauncher.scala:n ajokonfiguraatiohin:

Mene Run -> Edit Configurations -> Valitse EmbeddedJettyLauncher -> Modify Options -> Add VM Options
Ja lisää `-Dkouta-backend.config-profile=template -Dkouta-backend.embedded=false`

Vaihda myös PostgreSQL-tietokannan portti, käyttäjätunnus ja salasana `dev-vars.yml`-tiedostoon.

### Versiohallinta

Gitin kanssa on pyritty noudattamaan seuraavia käytänteitä:

- Commit viestien alkuun jira-tiketti, esim KTO-1215
- Branchit on nimetty jira-tiketin perusteella
- Jos masteriin on tullut committeja haaran tekemisen jälkeen, rebase on
  mergeä suositeltavampi tapa päivittää haara ajan tasalle
- Tekeminen on pyritty pilkkomaan mahdollisimman pieneksi, jotta haarat olisivat lyhytikäisiä (jos mahdollista, alle
  2 työpäivää)

## Ympäristöt

### Testiympäristöt

Testiympäristöjen swaggerit löytyvät seuraavista osoitteista:

- [untuva](https://virkailija.untuvaopintopolku.fi/kouta-backend/swagger)
- [hahtuva](https://virkailija.hahtuvaopintopolku.fi/kouta-backend/swagger)
- [QA eli pallero](https://virkailija.testiopintopolku.fi/kouta-backend/swagger)

### Asennus

Asennus hoituu samoilla työkaluilla kuin muidenkin OPH:n palvelujen.
[Cloud-basen dokumentaatiosta](https://github.com/Opetushallitus/cloud-base/tree/master/docs) ja ylläpidolta löytyy apuja.

### Buildaus haarasta

Github Actions tekee buildin jokaisesta pushista ja siirtää luodut paketit opetushallituksen [artifactoryyn](https://artifactory.opintopolku.fi/artifactory/#browse/search/maven).
Paketti luodaan aina master-haarasta. Mikäli tulee tarve sadaa paketointi kehityshaarasta, täytyy muuttaa 
`./.github/workflows/build.yml` -tiedostoa. Tällainen tilanne voi olla esimerkiksi jos tekee muutoksia kouta-backendin tietomalliin 
eikä vielä halua mergetä muutoksia masteriin, mutta tarvitsisi uutta tietomallia kuitenkin esimerkiksi kouta-internal tai kouta-external kehityshaaroissa. 
 
Tarvittava muutos `./.github/workflows/build.yml` tiedostoon on tällainen (`deploy_to_artifactory`-jobissa):
(myös tiedoston git historiasta voi katsoa mallia)

```diff
- if: github.ref == 'refs/heads/master'
+ if: github.ref == 'refs/heads/<haaran nimi>'
```

### Lokit

Kouta-backendin lokit löytyvät AWS:n cloudwatchista log groupista <testiympäristön nimi>-app-kouta-backend (esim. hahtuva-app-kouta-backend).
Lisäohjeita näihin ylläpidolta.


-----------------------VANHAN READMEN TIEDOT-----------------------

##### Konfigurointi

EmbeddedJettyLauncheria voidaan konfiguroida seuraavilla VM-parametreilla:
 
| System property                                 |                                                                                             |   
| ------------------------------------------------|:-------------------------------------------------------------------------------------------:|   
| ```-Dkouta-backend.port=xxxx```                 | Määrittää Jettyn portin (default 8099)                                                      |   
| ```-Dkouta-backend.embedded=xxxx```             | Käynnistetäänkö embedded PostgreSQL (default true)                                          |
| ```-Dkouta-backend.profile=xxxx```              | Määrittää profiilin                                                                         |   
| ```-Dkouta-backend.template=xxxx```             | Määrittää template-tiedoston polun                                                          |   

* Jos embedded Postgres ei ole käytössä, profiili voi olla joko *default* tai *template*
    * ```default```-profiilissa ```oph-configuration``` luetaan käyttäjän kotihakemistosta
    * ```template```-profiilissa asetukset rakennetaan .yml-templaten pohjalta
* Template-tiedoston polku on oletuksena ```'/src/test/resources/dev-vars.yml'``` 
* Jos siis haluaa käynnistää sovelluksen Ideassa ilman embedded-kantaa, pitää VM options -kentässä
  määritellä sekä embedded-kanta pois päältä että valita template-profiili:
  `-Dkouta-backend.embedded=false -Dkouta-backend.config-profile=template`
* Käytettäessä oikeaa S3:sta, tarvitsee olla käyttäjä tarpeellisilla oikeuksilla.
  `kouta-backend.awsKeys` arvoksi laitetaan true, jonka jälkeen avaimet luetaan AWS SDK:n oletuskäytännöllä,
  eli `AWS_ACCESS_KEY_ID` ja `AWS_SECRET_ACCESS_KEY` -ympäristömuuttujista tai `~/.aws/credentials` tiedostosta
  `AWS_PROFILE` -ymäristömuuttujan kertomasta profiilista.

### Riippuvuuksien tarkistaminen

```mvn enforcer:enforce```
