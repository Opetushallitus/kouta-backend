# kouta-backend

Uuden tarjonnan backend.

## Kehitysympäristön pystytys

Asenna haluamallasi tavalla koneellesi 
1. [IntelliJ IDEA](https://www.jetbrains.com/idea/) + [scala plugin](https://plugins.jetbrains.com/plugin/1347-scala)
2. [AWS cli](https://aws.amazon.com/cli/) (SQS-jonoja varten)
3. [Docker](https://www.docker.com/get-started) (localstackia ja postgresia varten)
4. [Maven](https://maven.apache.org/) Jos haluat ajaa komentoriviltä Mavenia, 
mutta idean Mavenilla pärjää kyllä hyvin, joten tämä ei ole pakollinen

Lisäksi tarvitset Java SDK:n ja Scala SDK:n (Unix pohjaisissa käyttöjärjestelmissä auttaa esim. [SDKMAN!](https://sdkman.io/)). Katso [.travis.yml](.travis.yml) mitä versioita sovellus käyttää. 
Kirjoitushetkellä käytössä openJDK8 (Java 11 käy myös) ja scala 2.12.10. 

### Postgresin ajaminen

Kontin luonti

``` shell
cd kouta-backend/postgresql/docker
docker build --tag kouta-postgres .
```

Kontin ajaminen

Tähän on kaksi vaihtoehtoa. Jos ei haittaa että kontin sammuttamisen jälkeen 
tietokantaan tallennetut tiedot häviävät, aja:

``` shell
docker run --rm --name kouta-database --env POSTGRES_PASSWORD=postgres -p 5432:5432 kouta-postgres
```

Jos haluat että tiedot säilyvät vaikka kontti sammutetaan, aja:

``` shell
docker volume create kouta-data # Tämä tarvitsee ajaa vain ensimmäisellä kerralla, myöhemmillä kerroilla riittää alle oleva komento
docker run --rm --name kouta-database --env POSTGRES_PASSWORD=postgres -p 5432:5432 --volume kouta-data:/var/lib/postgresql/data kouta-postgres
```

## Testit

Testit käyttävät localstackia ja postgresia joten Docker täytyy olla asennettuna 
ja kouta-postgres kontti käynnissä.

Testit voi ajaa ideassa ottamalla right-click kansion /kouta-backend/src/test/scala päällä -> Run 'ScalaTests in 'scala...'. 
Yksittäisen testisuiten tai testin voi ajaa ottamalla right-click halutun testiclassin tai funktion päältä. 

Jos Maven on asennettuna voi myös testit ajaa myös komentoriviltä `mvn test` komennolla tai rajaamalla ajettavien testejä
`mvn test -Dsuites="<testiluokan nimet pilkulla erotettuna>"`

## Ajaminen lokaalisti

Käynnistä kouta-postgres kontti ja Ideassa [EmbeddedJettyLauncher](embeddedJettyLauncher.scala) (right-click -> Run). 
Avaa Ideassa ylhäältä Run Configurations valikko ja aseta EmbeddedJettyLauncherin Working directoryksi `$MODULE_DIR$`. Sovellus
käynnistyy porttiin **8099** ja se käyttää postgres kontin kantaa. Asetuksia voi muuttaa muokkaamalla
```'/src/test/resources/dev-vars.yml'```-tiedostoa.

EmbeddedJettyLauncher luo automaattisesti myös SQS-jonot localstackiin porttiin localhost:4576.

Jos Docker ei ole ajossa EmbeddedJettyLauncher:n käynnistyksessä, niin saatat päätyä virheilmoitukseen
```com.amazonaws.SdkClientException: Unable to execute HTTP request: Read timed out```. 
Tästä voi toipua ajamalla `tools/stop_localstack`, joka poistaa "haamutiedoston".

## Indeksoinnin SQS-jonot

Indeksoinnin jonot saa manuaalisesti lokaalisti käyttöön porttiin `localhost:4576` käynnistämällä
Localstack Docker instanssin komennolla `tools/start_localstack` ja sammutettua
`tools/stop_localstack` komennolla. Mikäli käytössä on lokaali `konfo-indeksoija` voi sen
Localstack instanssia käyttää koska komennot ovat identtisiä.

Localstackin käynnistysskripti kirjoittaa `~/.kouta_localstack` -tiedostoon käynnistämänsä dockerin id:n.
Pysäytysskripti poistaa tuon tiedoston. Jos docker pysähtyy muulla tavalla, on mahdollista, että tuo tiedosto
jää paikoilleen, vaikka docker on jo sammunut. Silloin kannattaa ajaa `tools/stop_localstack`, joka poistaa "haamutiedoston".

##### Konfigurointi

EmbeddedJettyLauncheria voidaan konfiguroida seuraavilla VM-parametreilla:
 
| System property                     |                                                    |
| ----------------------------------- |:--------------------------------------------------:| 
| ```-Dkouta-backend.port=xxxx```     | Määrittää Jettyn portin (default 8099)             | 
| ```-Dkouta-backend.embedded=xxxx``` | Käynnistetäänkö embedded PostgreSQL (default true) |
| ```-Dkouta-backend.profile=xxxx```  | Määrittää profiilin                                | 
| ```-Dkouta-backend.template=xxxx``` | Määrittää template-tiedoston polun                 |
| ```-Dkouta-backend.awsKeys=xxxx```  | Luetaanko oikeat AWS-avaimet, oletuksena false     |

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

Lokaaliin kouta-backendiin saa generoitua testidataa ajamalla ```TestDataGenerator```.

### Swagger

[http://localhost:8099/kouta-backend/swagger/index.html](http://localhost:8099/kouta-backend/swagger/index.html)

### Riippuvuuksien tarkistaminen

```mvn enforcer:enforce```
