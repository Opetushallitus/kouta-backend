-- luonnostilaiset hakukohteet poistetaan
update hakukohteet
set tila = 'poistettu'
where toteutus_oid in (select t.oid
                       from toteutukset t,
                            koulutukset k,
                            hakukohteet h
                       where k.tyyppi in ('kk-opintojakso', 'kk-opintokokonaisuus')
                         and t.metadata::jsonb ->> 'hakulomaketyyppi' in ('muu', 'ei  sähköistä')
                         and t.oid = h.toteutus_oid
                         and t.koulutus_oid = k.oid
                         and t.tila <> 'poistettu'
                         and t.tila <> 'arkistoitu'
                         and h.tila = 'tallennettu'
                         and t.oid in (select new_oid from migration_old_to_new_oid_lookup));

-- lisätään hakukohteet käytössä -tieto toteutuksille joilla on hakukohteet käytössä
update toteutukset t
set metadata = jsonb_set(metadata, '{isHakukohteetKaytossa}', 'true', TRUE)
where t.metadata::jsonb ->> 'hakulomaketyyppi' = 'ataru'
  and t.metadata::jsonb ->> 'isHakukohteetKaytossa' is null
  and t.oid in (select toteutus_oid from hakukohteet)
  and t.tila<>'poistettu' and t.tila<>'arkistoitu'