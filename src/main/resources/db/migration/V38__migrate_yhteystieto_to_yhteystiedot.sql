update toteutukset set metadata = jsonb_set( metadata,
                                             '{yhteystiedot}',
                                             to_jsonb(json_build_array((metadata->'yhteystieto'))))
where metadata->'yhteystieto' IS NOT NULL;

update toteutukset set metadata = metadata #- '{yhteystieto}';

update haut set metadata = jsonb_set( metadata,
                                      '{yhteystiedot}',
                                      to_jsonb(json_build_array((metadata->'yhteystieto'))))
where metadata->'yhteystieto' IS NOT NULL;

update haut set metadata = metadata #- '{yhteystieto}';