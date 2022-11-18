
update koulutukset
set metadata = jsonb_set(metadata, '{jarjestajat}', to_jsonb(metadata -> 'orgsAllowedToReadKoulutus'), TRUE)
where metadata -> 'orgsAllowedToReadKoulutus' notnull;

update koulutukset
set metadata = metadata #- '{orgsAllowedToReadKoulutus}'
where metadata -> 'orgsAllowedToReadKoulutus' notnull;

update koulutukset
set metadata = jsonb_set(metadata, '{isAvoinKorkeakoulutus}', to_jsonb(metadata -> 'avoinKorkeakoulutus'), TRUE)
where metadata -> 'avoinKorkeakoulutus' notnull;

update koulutukset
set metadata = metadata #- '{avoinKorkeakoulutus}'
where metadata -> 'avoinKorkeakoulutus' notnull;