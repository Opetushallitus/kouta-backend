alter table koulutukset
  alter column tyyppi drop not null,
  alter column koulutus_koodi_uri drop not null,
  alter column nimi drop not null;

alter table koulutukset_history
  alter column tyyppi drop not null,
  alter column koulutus_koodi_uri drop not null,
  alter column nimi drop not null;

alter table toteutukset
  alter column nimi drop not null;

alter table toteutukset_history
  alter column nimi drop not null;