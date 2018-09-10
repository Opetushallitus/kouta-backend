SET client_encoding = 'UTF8';


CREATE TABLE komo (
  oid character varying PRIMARY KEY,
  koulutus character varying NOT NULL,
  nimi character varying
);
ALTER TABLE komo OWNER TO oph;