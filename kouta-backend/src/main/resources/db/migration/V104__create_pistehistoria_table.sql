create table if not exists pistehistoria (
    tarjoaja_oid varchar not null,
    hakukohdekoodi varchar not null,
    vuosi varchar not null,
    pisteet numeric not null,
    valintatapajono_oid varchar,
    hakukohde_oid varchar,
    haku_oid varchar,
    updated TIMESTAMP not null default now(),
    primary key (tarjoaja_oid, hakukohdekoodi, vuosi)
);