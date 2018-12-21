# kouta-backend

Uuden tarjonnan backend.

## Ajaminen lokaalisti

Käynnistä Ideassa ```EmbeddedJettyLauncher```. Sovellus käynnistyy porttiin **8099** ja se käyttää embedded Postgres-kantaa.
Asetuksia voi muuttaa muokkaamalla ```'/src/test/resources/dev-vars.yml'```-tiedostoa.

Emebedded Postgres-kannan käyttäminen vaatii, että postgresql on asennettu koneelle.

##### Konfigurointi

EmbeddedJettyLauncheria voidaan konfiguroida seuraavilla VM-parametreilla:
 
| System property |   |
| ----------------------------------- |:--------------------------------------------------:| 
| ```-Dkouta-backend.port=xxxx```     | Määrittää Jettyn portin (default 8099)             | 
| ```-Dkouta-backend.embedded=xxxx``` | Käynnistetäänkö embedded PostgreSQL (default true) |
| ```-Dkouta-backend.profile=xxxx```  | Määrittää profiilin                                | 
| ```-Dkouta-backend.template=xxxx``` | Määrittää template-tiedoston polun                 |

* Jos embedded Postgres ei ole käytössä, profiili voi olla joko *default* tai *template*
    * ```default```-profiilissa ```oph-configuration``` luetaan käyttäjän kotihakemistosta
    * ```template```-profiilissa asetukset rakennetaan .yml-templaten pohjalta
* Template-tiedoston polku on oletuksena ```'/src/test/resources/dev-vars.yml'``` 

### Testidatan generointi

Lokaaliin kouta-backendiin saa generoitua testidataa ajamalla ```TestDataGenerator```.

### Swagger

[http://localhost:8099/kouta-backend/swagger/index.html](http://localhost:8099/kouta-backend/swagger/index.html)

### Riippuvuuksien tarkistaminen

```mvn enforcer:enforce```
