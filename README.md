# kouta-backend

Uuden tarjonnan backend.

### Ajaminen lokaalisti

Käynnistä Ideassa JettyLauncher. Sovellus käynnistyy porttiin **8099**, ellei porttia ole vaihdettu VM optionsilla ```-Dkouta-backend.port=xxxx```

### Swagger

[http://localhost:8099/kouta-backend/swagger/index.html](http://localhost:8099/kouta-backend/swagger/index.html)

### Riippuvuuksien tarkistaminen

```mvn enforcer:enforce```