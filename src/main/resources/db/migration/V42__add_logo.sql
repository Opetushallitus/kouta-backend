-- Koulutukset

alter table oppilaitokset add column logo varchar;

comment on column oppilaitokset.logo is 'URL oppilaitoksen logoon';
