with jarjestaa_urheilijan_amm_koulutusta as (
    select oo.oppilaitos_oid as oid, bool_or((metadata->>'jarjestaaUrheilijanAmmKoulutusta')::boolean) as result_
    from oppilaitosten_osat oo
    group by oo.oppilaitos_oid
)
update oppilaitokset o
set metadata = jsonb_set(
    metadata,
    '{jarjestaaUrheilijanAmmKoulutusta}',
    to_jsonb(
        coalesce(
            (select j.result_
             from jarjestaa_urheilijan_amm_koulutusta j
             where j.oid = o.oid),
            false)));