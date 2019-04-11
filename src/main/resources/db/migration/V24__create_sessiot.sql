create table sessiot(
  id              uuid primary key,
  cas_tiketti     character varying,
  henkilo         character varying not null,
  viimeksi_luettu timestamptz       not null default now()
);

alter table sessiot
  owner to oph;

create table roolit(
  sessio uuid references sessiot (id) on delete cascade,
  rooli  character varying not null
);

alter table roolit
  owner to oph;

create index roolit_sessio_idx ON roolit (sessio);
