begin;

with maksullisuustyyppi as (
        select oid, metadata#>'{opetus, maksullisuustyyppi}' as mt, metadata#>'{opetus, maksunMaara}' as maksunMaara
        from toteutukset t)
update toteutukset t
set metadata = jsonb_set(metadata, '{opetus, maksut}', (
    select jsonb_agg(
        jsonb_build_object(
            'maksullisuustyyppi', maksullisuustyyppi.mt,
            'maksunMaara', maksullisuustyyppi.maksunMaara
        )
    )
    from maksullisuustyyppi
    where t."oid" = maksullisuustyyppi.oid
))
where metadata#>>'{opetus, maksullisuustyyppi}' is not null;

update toteutukset t
set metadata = metadata #- '{opetus, maksullisuustyyppi}'
where metadata#>>'{opetus, maksullisuustyyppi}' is not null;

update toteutukset t
set metadata = metadata #- '{opetus, maksunMaara}'
where metadata#>>'{opetus, maksunMaara}' is not null;

commit;