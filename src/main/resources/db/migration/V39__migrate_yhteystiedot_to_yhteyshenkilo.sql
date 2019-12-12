update haut set metadata = jsonb_set(metadata, '{yhteyshenkilot}', metadata->'yhteystiedot') where metadata->'yhteystiedot' IS NOT NULL;
update haut set metadata = metadata #- '{yhteystiedot}';

update toteutukset set metadata = jsonb_set(metadata, '{yhteyshenkilot}', metadata->'yhteystiedot') where metadata->'yhteystiedot' IS NOT NULL;
update toteutukset set metadata = metadata #- '{yhteystiedot}';