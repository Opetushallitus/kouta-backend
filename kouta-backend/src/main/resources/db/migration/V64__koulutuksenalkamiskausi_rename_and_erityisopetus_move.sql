-- Varmistetaan että vanhat koulutuksenAlkamiskausi-kentät on poistettu
update toteutukset
set metadata = metadata #- '{opetus, koulutuksenAlkamiskausi}'
where jsonb_typeof(metadata -> 'opetus' -> 'koulutuksenAlkamiskausi') != 'object';

update toteutukset
set metadata = metadata #- '{opetus, koulutuksenTarkkaAlkamisaika}';

update toteutukset
set metadata = metadata #- '{opetus, koulutuksenAlkamispaivamaara}';

update toteutukset
set metadata = metadata #- '{opetus, koulutuksenPaattymispaivamaara}';

update toteutukset
set metadata = metadata #- '{opetus, koulutuksenAlkamisvuosi}';

-- Kopioidaan koulutuksenAlkamiskausiUUSI-kentästä tiedot koulutuksenAlkamiskausi-kenttään
update toteutukset
set metadata = jsonb_set(metadata, '{opetus, koulutuksenAlkamiskausi}', to_jsonb(metadata -> 'opetus' -> 'koulutuksenAlkamiskausiUUSI'), TRUE)
where metadata -> 'opetus' -> 'koulutuksenAlkamiskausiUUSI' notnull;

-- Poistetaan koulutuksenAlkamiskausiUUSI-kenttä
update toteutukset
set metadata = metadata #- '{opetus, koulutuksenAlkamiskausiUUSI}';

-- Siirretään ammatillinenPerustutkintoErityisopetuksena-kenttä opetuksen alta toteutuksen metadatan päätasolle ja ainoastaan ammatillisille koulutuksille
update toteutukset
set metadata = jsonb_set(metadata, '{ammatillinenPerustutkintoErityisopetuksena}', to_jsonb(metadata -> 'opetus' -> 'ammatillinenPerustutkintoErityisopetuksena'), TRUE)
where metadata ->> 'tyyppi' = 'amm' and (metadata -> 'opetus' -> 'ammatillinenPerustutkintoErityisopetuksena') is not null;

-- Poistetaan ammatillinenPerustutkintoErityisopetuksena-kenttä opetuksen alta
update toteutukset
set metadata = metadata #- '{opetus, ammatillinenPerustutkintoErityisopetuksena}';