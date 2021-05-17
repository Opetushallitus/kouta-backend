# kouta-backend

## 1. Palvelun tehtävä

Uuden koulutustarjonnan datan hallinnoija. Sisältää koulutustarjonnan master-datan, jota kouta-indeksoija rikastaa ja
jakelee muille palveluille.

## 2. Arkkitehtuuri

Kouta-backend on Scalalla ja Scalatra frameworkilla toteutettu web api. Kouta-ui:n kautta syötetään koulutustarjonnan
tiedot, jotka kouta-backend validoi ja tallentaa. Tallennuksen yhteydessä lähetetään sqs-viesti jonoon jota 
[kouta-indeksoija](https://github.com/Opetushallitus/kouta-indeksoija) kuuntelee. Viestissä on tallennetun entiteetin 
id, jonka avulla [kouta-indeksoija](https://github.com/Opetushallitus/kouta-indeksoija) kysyy kouta-backendilta entiteetin tarkemmat
tiedot.

Kouta-backendin tarkoitus on olla uuden koulutustarjonnan tietomallin vartija. Tänne ei duplikoida muiden palvelujen dataa,
vaan muihin palveluihin viittaavasta datasta tallennetaan ainoastaan id (esim. eperusteId). 
[Kouta-indeksoija](https://github.com/Opetushallitus/kouta-indeksoija) huolehtii näiden id-arvojen avulla tietojen 
rikastamisesta esim. oppijan käyttäliittymää ([konfo-ui](https://github.com/Opetushallitus/konfo-ui)) varten.

Kouta-backendin data tallennetaan PostgreSQL-kantaan. Tietokantarakenteessa on ilmeisesti haluttu välttää vanhan tarjonnan
monimutkaisuus ja siksi skeema on haluttu pitää mahdollisimman flattina. Tästä johtuen skeema on sekoitus relaatiotietokantaa ja
dokumenttitietokantaa. Tämä on totetettu niin että entiteetin tärkeimmät kentät on taulun päätasolla ja loput kentät metadata nimisessä
kentässä jsonb-formaatissa olevassa tietorakenteessa.

Kouta-backendin käyttämät tietolähteet:

| Tietolähde                                                   | Haettavat tiedot |
|--------------------------------------------------------------|------------------|
| [kouta-index](https://github.com/Opetushallitus/kouta-index) | Haetaan kouta-ui:n etusivun listauksiin rikastettua tietoa (tämä palvelu pyritään poistamaan tulevaisuudessa) |
| käyttöoikeuspalvelu                                          | Tarkastetaan käyttäjän käyttöoikeudet |
| ohjausparametritpalvelu                                      | Asetetaan hakujen ohjausparametrit |
| organisaatiopalvelu                                          | Koulutustoimijoiden, oppilaitosten ja toimipisteiden organisaatiohierarkia |

## 3. Kehitysympäristö

### 3.1. Esivaatimukset

Asenna haluamallasi tavalla koneellesi
1. [IntelliJ IDEA](https://www.jetbrains.com/idea/) + [scala plugin](https://plugins.jetbrains.com/plugin/1347-scala)
2. [AWS cli](https://aws.amazon.com/cli/) (SQS-jonoja varten)
3. [Docker](https://www.docker.com/get-started) (localstackia ja postgresia varten)
4. [Maven](https://maven.apache.org/) Jos haluat ajaa komentoriviltä Mavenia,
   mutta idean Mavenilla pärjää kyllä hyvin, joten tämä ei ole pakollinen

Lisäksi tarvitset Java SDK:n ja Scala SDK:n (Unix pohjaisissa käyttöjärjestelmissä auttaa esim. [SDKMAN!](https://sdkman.io/)). Katso [.travis.yml](.travis.yml) mitä versioita sovellus käyttää.
Kirjoitushetkellä käytössä openJDK8 (Java 11 käy myös) ja scala 2.12.10.

PostgreSQL Kontti-imagen luonti (tarvitsee tehdä vain kerran):

``` shell
cd kouta-backend/postgresql/docker
docker build --tag kouta-postgres .
```

### 3.2. Testien ajaminen

Jos Maven on asennettuna voi testit ajaa komentoriviltä `mvn test` komennolla tai rajaamalla ajettavien testejä
`mvn test -Dsuites="<testiluokan nimet pilkulla erotettuna>"`

Testit voi ajaa ideassa Maven ikkunasta valitsemalla test kouta-backend-parentin kohdalta tai
avaamalla Edit Configurations valikon ja luomalla uuden Maven run configurationin jolle
laitetaan working directoryksi projektin juurikansio ja Command line komennoksi `test`. Tämän jälkeen konfiguraatio ajoon.

Yksittäisen testisuiten tai testin voi ajaa ottamalla right-click halutun testiclassin tai funktion päältä.

Testit käynnistävät PostgreSQL:n docker-kontissa satunnaiseen vapaaseen porttiin.

#### 3.2.1 Testit ja Windows + docker

Jotkin testit (IndexerSpec) voivat epäonnistua windowsilla, koska getDockerExeLocation olettaa `docker.exe` löytyvän kovakoodatusti tietystä polusta:

- `C:/program files/docker/docker/resources/bin/docker.exe`

Tähän voi tehdä korjauksen ainakin seuraavalla tavalla: lisää ko. polkuun symbolinen linkki viittaamaan oikeaan polkuun, mistä docker.exe löytyy. Tämän saa helpoiten tehtyä windowsissa pikakuvakkeella.

### 3.3. Migraatiot

Tietokantamigraatiot on toteutettu [flywaylla](https://flywaydb.org/) ja ajetaan automaattisesti testien ja asennuksen 
yhteydessä. Migraatiotiedostot löytyvät kansiosta `kouta-backend/src/main/resources/db/migration`

### 3.4. Ajaminen lokaalisti

Käynnistä Ideassa ```embeddedJettyLauncher.scala``` (right-click -> Run).
Avaa Ideassa ylhäältä Run Configurations valikko ja aseta EmbeddedJettyLauncherin Working directoryksi `$MODULE_DIR$` 
(Jostain syystä IDEA laittaa tämän väärin ja siksi pitää käsin käydä päivittämässä). Sovellus
käynnistyy porttiin **8099** ja se käyttää valittua postgres kantaa (host tai kontti).
Asetuksia voi muuttaa muokkaamalla
```'/src/test/resources/dev-vars.yml'```-tiedostoa.

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

### 3.5. Versiohallinta

Gitin kanssa on pyritty noudattamaan seuraavia käytänteitä:

- Commit viestien alkuun jira-tiketti, esim KTO-1215
- Branchit on nimetty jira-tiketin perusteella
- Jos masteriin on tullut committeja haaran tekemisen jälkeen, rebase on
  mergeä suositeltavampi tapa päivittää haara ajan tasalle
- Tekeminen on pyritty pilkkomaan mahdollisimman pieneksi, jotta haarat olisivat lyhytikäisiä (jos mahdollista, alle 
  2 työpäivää)

TODO ympäristöt, lokit, travis, asennus, riippuvuudet

-----------------------VANHAN READMEN TIEDOT-----------------------

##### Konfigurointi

EmbeddedJettyLauncheria voidaan konfiguroida seuraavilla VM-parametreilla:
 
| System property                                 |                                                                                             |   
| ------------------------------------------------|:-------------------------------------------------------------------------------------------:|   
| ```-Dkouta-backend.port=xxxx```                 | Määrittää Jettyn portin (default 8099)                                                      |   
| ```-Dkouta-backend.embedded=xxxx```             | Käynnistetäänkö embedded PostgreSQL (default true)                                          |   
| ```-Dkouta-backend.embeddedPostgresType=xxxx``` | Käynnistetäänkö PostgreSQL host-koneella vai kontissa (`host` tai `docker`, default docker) |   
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

### Testidatan generointi

Lokaaliin kouta-backendiin saa generoitua testidataa ajamalla ```TestDataGenerator```. (Tämä on vanhentunut ja ei toimi enää)

### Riippuvuuksien tarkistaminen

```mvn enforcer:enforce```
