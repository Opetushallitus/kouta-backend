-- Luo tyhjän koulutuksen alkamiskauden jos jossakin vanhassa kentässä arvo
update toteutukset
set metadata = jsonb_set(metadata, '{opetus,koulutuksenAlkamiskausiUUSI}', '{}', TRUE)
where (metadata -> 'opetus' ->> 'koulutuksenTarkkaAlkamisaika' notnull or
    metadata -> 'opetus' ->> 'koulutuksenAlkamispaivamaara' notnull or
    metadata -> 'opetus' ->> 'koulutuksenPaattymispaivamaara' notnull or
    metadata -> 'opetus' ->> 'koulutuksenAlkamiskausi' notnull or
    metadata -> 'opetus' ->> 'koulutuksenAlkamisvuosi' notnull);

-- päivittää tyypiksi "tarkka alkamisajankohta" jos vanha koulutuksenTarkkaAlkamisaika on true
update toteutukset
set metadata = jsonb_set(metadata, '{opetus,koulutuksenAlkamiskausiUUSI,alkamiskausityyppi}', '"tarkka alkamisajankohta"', TRUE)
where (metadata -> 'opetus' ->> 'koulutuksenTarkkaAlkamisaika' notnull and metadata -> 'opetus' ->> 'koulutuksenTarkkaAlkamisaika' = 'true');

-- päivittää tyypiksi "alkamiskausi ja -vuosi" jos vanha koulutuksenTarkkaAlkamisaika on false
update toteutukset
set metadata = jsonb_set(metadata, '{opetus,koulutuksenAlkamiskausiUUSI,alkamiskausityyppi}', '"alkamiskausi ja -vuosi"', TRUE)
where (metadata -> 'opetus' ->> 'koulutuksenTarkkaAlkamisaika' = 'false');

-- kopioitu alkamiskausiKoodiUri vanhasta koulutuksenAlkamiskausi-kentästä
update toteutukset
set metadata = jsonb_set(metadata, '{opetus,koulutuksenAlkamiskausiUUSI,koulutuksenAlkamiskausiKoodiUri}', to_jsonb(metadata -> 'opetus' ->> 'koulutuksenAlkamiskausi'), TRUE)
where metadata -> 'opetus' ->> 'koulutuksenAlkamiskausi' notnull;

-- kopioitu koulutuksenAlkamisvuosi vanhasta koulutuksenAlkamisvuosi-kentästä
update toteutukset
set metadata = jsonb_set(metadata, '{opetus,koulutuksenAlkamiskausiUUSI,koulutuksenAlkamisvuosi}', to_jsonb(metadata -> 'opetus' ->> 'koulutuksenAlkamisvuosi'), TRUE)
where metadata -> 'opetus' ->> 'koulutuksenAlkamisvuosi' notnull;

-- kopioitu koulutuksenAlkamispaivamaara vanhasta koulutuksenAlkamispaivamaara-kentästä
update toteutukset
set metadata = jsonb_set(metadata, '{opetus,koulutuksenAlkamiskausiUUSI,koulutuksenAlkamispaivamaara}', to_jsonb(metadata -> 'opetus' ->> 'koulutuksenAlkamispaivamaara'), TRUE)
where metadata -> 'opetus' ->> 'koulutuksenAlkamispaivamaara' notnull;

-- kopioitu koulutuksenPaattymispaivamaara vanhasta koulutuksenPaattymispaivamaara-kentästä
update toteutukset
set metadata = jsonb_set(metadata, '{opetus,koulutuksenAlkamiskausiUUSI,koulutuksenPaattymispaivamaara}', to_jsonb(metadata -> 'opetus' ->> 'koulutuksenPaattymispaivamaara'), TRUE)
where metadata -> 'opetus' ->> 'koulutuksenPaattymispaivamaara' notnull;