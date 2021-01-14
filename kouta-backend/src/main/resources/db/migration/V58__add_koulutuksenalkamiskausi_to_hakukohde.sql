update hakukohteet
set metadata = jsonb_set(metadata, '{koulutuksenAlkamiskausi}', '{}', TRUE)
where (alkamiskausi_koodi_uri notnull or alkamisvuosi notnull );

update hakukohteet
set metadata = jsonb_set(metadata, '{koulutuksenAlkamiskausi,alkamiskausityyppi}', '"alkamiskausi ja -vuosi"', TRUE)
where (alkamiskausi_koodi_uri notnull or alkamisvuosi notnull );

update hakukohteet
set metadata = jsonb_set(metadata, '{koulutuksenAlkamiskausi,koulutuksenAlkamiskausiKoodiUri}', to_jsonb(alkamiskausi_koodi_uri), TRUE)
where alkamiskausi_koodi_uri notnull;

update hakukohteet
set metadata = jsonb_set(metadata, '{koulutuksenAlkamiskausi,koulutuksenAlkamisvuosi}', to_jsonb(alkamisvuosi), TRUE)
where alkamisvuosi notnull;

update hakukohteet
set metadata = jsonb_set(metadata, '{kaytetaanHaunAlkamiskautta}', to_jsonb(kaytetaan_haun_alkamiskautta), TRUE);
