update toteutukset
set metadata = jsonb_set(metadata, '{opetus, maksullisuustyyppi}', '"maksuton"', TRUE);

update toteutukset
set metadata = jsonb_set(metadata, '{opetus, maksullisuustyyppi}', '"maksullinen"', TRUE)
where metadata -> 'opetus' ->> 'onkoMaksullinen' = 'true';

update toteutukset
set metadata = metadata #- '{opetus, onkoMaksullinen}';
