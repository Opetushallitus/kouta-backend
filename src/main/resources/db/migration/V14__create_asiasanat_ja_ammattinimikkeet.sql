create type kieli as enum (
  'fi',
  'sv',
  'en'
);
alter type kieli owner to oph;

create table asiasanat (
  asiasana varchar not null,
  kieli kieli not null,
  primary key (asiasana, kieli)
);
alter table asiasanat owner to oph;

create table ammattinimikkeet (
  ammattinimike varchar not null,
  kieli kieli not null,
  primary key (ammattinimike, kieli)

);
alter table ammattinimikkeet owner to oph;