-- Koulutukset

alter table koulutukset
    add column teemakuva varchar;

comment on column koulutukset.teemakuva is 'URL koulutuksen teemakuvaan';

update koulutukset
set teemakuva = q1.teemakuva
from (select oid, metadata ->> 'teemakuva' as teemakuva
      from koulutukset
      where metadata ->> 'teemakuva' is not null) as q1
where koulutukset.oid = q1.oid;

update koulutukset set metadata = metadata #- '{teemakuva}' where metadata->teemakuva is not null;

-- Toteutukset

alter table toteutukset
    add column teemakuva varchar;

comment on column toteutukset.teemakuva is 'URL toteutuksen teemakuvaan';

update toteutukset
set teemakuva = q1.teemakuva
from (select oid, metadata ->> 'teemakuva' as teemakuva
      from toteutukset
      where metadata ->> 'teemakuva' is not null) as q1
where toteutukset.oid = q1.oid;

update toteutukset set metadata = metadata #- '{teemakuva}' where metadata->teemakuva is not null;

-- Oppilaitokset

alter table oppilaitokset
    add column teemakuva varchar;

comment on column oppilaitokset.teemakuva is 'URL oppilaitoksen teemakuvaan';

update oppilaitokset
set teemakuva = q1.teemakuva
from (select oid, metadata ->> 'teemakuva' as teemakuva
      from oppilaitokset
      where metadata ->> 'teemakuva' is not null) as q1
where oppilaitokset.oid = q1.oid;

update oppilaitokset set metadata = metadata #- '{teemakuva}' where metadata->teemakuva is not null;

-- Oppilaitosten osat

alter table oppilaitosten_osat
    add column teemakuva varchar;

comment on column oppilaitosten_osat.teemakuva is 'URL oppilaitoksen osan teemakuvaan';

update oppilaitosten_osat
set teemakuva = q1.teemakuva
from (select oid, metadata ->> 'teemakuva' as teemakuva
      from oppilaitosten_osat
      where metadata ->> 'teemakuva' is not null) as q1
where oppilaitosten_osat.oid = q1.oid;

update oppilaitosten_osat set metadata = metadata #- '{teemakuva}' where metadata->teemakuva is not null;
