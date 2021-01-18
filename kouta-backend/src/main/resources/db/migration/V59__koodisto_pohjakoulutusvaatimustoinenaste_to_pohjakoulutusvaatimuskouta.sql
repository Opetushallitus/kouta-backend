-- Vaihdetaan pohjakoulutusvaatimus koodiarvoissa ensin koodiston nimi uuteen
update hakukohteet set pohjakoulutusvaatimus_koodi_urit = array(
	select regexp_replace(unnest(pohjakoulutusvaatimus_koodi_urit), 'pohjakoulutusvaatimustoinenaste(.*)' , 'pohjakoulutusvaatimuskouta\1', 'g'));

-- Jaetaan koodiarvo "pkyo" erillisiksi koodiarvoiksi
update hakukohteet
set pohjakoulutusvaatimus_koodi_urit = array_cat(pohjakoulutusvaatimus_koodi_urit, '{pohjakoulutusvaatimuskouta_pk#1, pohjakoulutusvaatimuskouta_yo#1}')
where pohjakoulutusvaatimus_koodi_urit @> '{pohjakoulutusvaatimuskouta_pkyo#1}';

-- Jaetaan koodiarvo "yoam" erillisiksi koodiarvoiksi
-- Ammatillinen koulutus (am) on joko Ammatillinen perustutkinto (104), Ammattitutkinto (105) tai Erikoisammattitutkinto (125)
update hakukohteet
set pohjakoulutusvaatimus_koodi_urit = array_cat(pohjakoulutusvaatimus_koodi_urit, '{pohjakoulutusvaatimuskouta_yo#1, pohjakoulutusvaatimuskouta_104#1, pohjakoulutusvaatimuskouta_105#1, pohjakoulutusvaatimuskouta_125#1}')
where pohjakoulutusvaatimus_koodi_urit @> '{pohjakoulutusvaatimuskouta_yoam#1}';

-- Poistetaan vanhat "pkyo" ja "yoam" yhdistelmäarvot
update hakukohteet
set pohjakoulutusvaatimus_koodi_urit = array_remove(pohjakoulutusvaatimus_koodi_urit, 'pohjakoulutusvaatimuskouta_pkyo#1');
update hakukohteet
set pohjakoulutusvaatimus_koodi_urit = array_remove(pohjakoulutusvaatimus_koodi_urit, 'pohjakoulutusvaatimuskouta_yoam#1');

-- Poistetaan duplikaatit ja järjestetään koodiurin mukaan
update hakukohteet
set pohjakoulutusvaatimus_koodi_urit = array(select distinct unnest(pohjakoulutusvaatimus_koodi_urit) as x order by x);