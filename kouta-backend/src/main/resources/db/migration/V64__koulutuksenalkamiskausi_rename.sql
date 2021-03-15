-- Varmistetaan että koulutuksenAlkamiskausi-kentässä ei ole enää vanhaa dataa
update toteutukset
set metadata = metadata #- '{opetus, koulutuksenAlkamiskausi}';

-- Kopioidaan koulutuksenAlkamiskausiUUSI-kentästä tiedot koulutuksenAlkamiskausi-kenttään
update toteutukset
set metadata = jsonb_set(metadata, '{opetus, koulutuksenAlkamiskausi}', to_jsonb(metadata -> 'opetus' -> 'koulutuksenAlkamiskausiUUSI'), TRUE)
where metadata -> 'opetus' -> 'koulutuksenAlkamiskausiUUSI' notnull;

-- Poistetaan koulutuksenAlkamiskausiUUSI-kenttä
update toteutukset
set metadata = metadata #- '{opetus, koulutuksenAlkamiskausiUUSI}';