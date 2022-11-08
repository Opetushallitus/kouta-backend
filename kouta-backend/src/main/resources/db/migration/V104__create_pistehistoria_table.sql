create table if not exists pistehistoria (
    tarjoajaOid varchar not null,
    hakukohdekoodi varchar not null,
    vuosi varchar not null,
    pisteet numeric not null,
    valintatapajonoOid varchar,
    hakukohdeOid varchar,
    hakuOid varchar,
    primary key (tarjoajaOid, hakukohdekoodi, vuosi)
);