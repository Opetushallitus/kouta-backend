do
$BODY$
    declare
        result       record;
        old_metadata jsonb;
        new_metadata jsonb;
    begin
        for result in select hk.oid, hk.metadata, hk.metadata::jsonb -> 'uudenOpiskelijanUrl' as url, hk.kielivalinta
                      from hakukohteet hk
                      where metadata::jsonb -> 'uudenOpiskelijanUrl' is not null
            loop
                old_metadata = result.metadata;
                new_metadata = jsonb_set(old_metadata, '{uudenOpiskelijanUrl}', '{}');

                if result.kielivalinta ? 'fi' then
                    new_metadata = jsonb_insert(new_metadata, '{uudenOpiskelijanUrl, fi}', result.url);
                end if;

                if result.kielivalinta ? 'sv' then
                    new_metadata = jsonb_insert(new_metadata, '{uudenOpiskelijanUrl, sv}', result.url);
                end if;

                if result.kielivalinta ? 'en' then
                    new_metadata = jsonb_insert(new_metadata, '{uudenOpiskelijanUrl, en}', result.url);
                end if;

                update hakukohteet set metadata = new_metadata where oid = result.oid;
            end loop;
    end;
$BODY$ language plpgsql;