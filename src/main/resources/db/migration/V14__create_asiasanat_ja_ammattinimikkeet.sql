create type kieli as enum (
  'fi',
  'sv',
  'en'
);

create table asiasanat (
  asiasana varchar not null,
  kieli kieli not null,
  primary key (asiasana, kieli)
);

create table ammattinimikkeet (
  ammattinimike varchar not null,
  kieli kieli not null,
  primary key (ammattinimike, kieli)

);