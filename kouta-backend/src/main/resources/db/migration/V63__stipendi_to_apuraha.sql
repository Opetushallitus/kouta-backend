update toteutukset
set metadata = jsonb_set(metadata, '{opetus, apuraha}', '{}', TRUE);

update toteutukset
set metadata = jsonb_set(metadata, '{opetus, onkoApuraha}', 'false', TRUE);

update toteutukset
set metadata = jsonb_set(metadata, '{opetus, onkoApuraha}', 'true', TRUE)
where (metadata -> 'opetus' ->> 'onkoStipendia' notnull and metadata -> 'opetus' ->> 'onkoStipendia' = 'true');

update toteutukset
set metadata = jsonb_set(metadata, '{opetus, apuraha, min}', (metadata -> 'opetus' -> 'stipendinMaara'), TRUE)
where metadata -> 'opetus' ->> 'stipendinMaara' notnull;

update toteutukset
set metadata = jsonb_set(metadata, '{opetus, apuraha, max}', (metadata -> 'opetus' -> 'stipendinMaara'), TRUE)
where metadata -> 'opetus' ->> 'stipendinMaara' notnull;

update toteutukset
set metadata = jsonb_set(metadata, '{opetus, apuraha, yksikko}', '"euro"', TRUE)
where metadata -> 'opetus' ->> 'stipendinMaara' notnull;

update toteutukset
set metadata = jsonb_set(metadata, '{opetus,apuraha,kuvaus}', to_jsonb(metadata -> 'opetus' -> 'stipendinKuvaus'), TRUE)
where metadata -> 'opetus' ->> 'stipendinKuvaus' notnull;

update toteutukset
set metadata = metadata #- '{opetus, onkoStipendia}';

update toteutukset
set metadata = metadata #- '{opetus, stipendinMaara}';

update toteutukset
set metadata = metadata #- '{opetus, stipendinKuvaus}';
