-- Muutetaan oppilaitosten yhteystiedot taulukoksi
-- Jälkimmäinen update tarvitaan jottei syntyisi [null] taulukoita niille joilla ei ole yhteystietoja
UPDATE oppilaitokset
SET metadata = jsonb_set(metadata #- '{yhteystiedot,osoite}',
                                     '{yhteystiedot,postiosoite}',
                         metadata #> '{yhteystiedot,osoite}')
WHERE metadata -> 'yhteystiedot' -> 'osoite' is not null;

UPDATE oppilaitokset
SET metadata = jsonb_set(metadata, '{yhteystiedot}', to_jsonb(json_build_array(metadata -> 'yhteystiedot')), TRUE)
WHERE metadata -> 'yhteystiedot' IS NOT NULL AND
      jsonb_typeof(metadata -> 'yhteystiedot') != 'array';

UPDATE oppilaitokset
SET metadata = jsonb_set(metadata, '{yhteystiedot}', to_jsonb(json_build_array()), TRUE)
WHERE metadata -> 'yhteystiedot' IS NULL;

-- Sama oppilaitosten osille
UPDATE oppilaitosten_osat
SET metadata = jsonb_set(metadata #- '{yhteystiedot,osoite}',
                                     '{yhteystiedot,postiosoite}',
                         metadata #> '{yhteystiedot,osoite}')
WHERE metadata -> 'yhteystiedot' -> 'osoite' is not null;

UPDATE oppilaitosten_osat
SET metadata = jsonb_set(metadata, '{yhteystiedot}', to_jsonb(json_build_array(metadata -> 'yhteystiedot')), TRUE)
WHERE metadata -> 'yhteystiedot' IS NOT NULL AND
      jsonb_typeof(metadata -> 'yhteystiedot') != 'array';

UPDATE oppilaitosten_osat
SET metadata = jsonb_set(metadata, '{yhteystiedot}', to_jsonb(json_build_array()), TRUE)
WHERE metadata -> 'yhteystiedot' IS NULL;