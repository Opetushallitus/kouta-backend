alter table koulutukset add column julkinen boolean not null default false;
alter table koulutukset_history add column julkinen boolean not null default false;

create or replace function update_koulutukset_history() returns trigger as
$$
begin
  insert into koulutukset_history (
      oid,
      johtaa_tutkintoon,
      tyyppi,
      koulutus_koodi_uri,
      tila,
      nimi,
      julkinen,
      metadata,
      muokkaaja,
      organisaatio_oid,
      kielivalinta,
      transaction_id,
      system_time
      ) values (
                   old.oid,
                   old.johtaa_tutkintoon,
                   old.tyyppi,
                   old.koulutus_koodi_uri,
                   old.tila,
                   old.nimi,
                   old.julkinen,
                   old.metadata,
                   old.muokkaaja,
                   old.organisaatio_oid,
                   old.kielivalinta,
                   old.transaction_id,
                   tstzrange(lower(old.system_time), now(), '[)')
                   );
  return null;
end;
$$ language plpgsql;