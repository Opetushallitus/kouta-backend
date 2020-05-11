create table sessions (
  id         uuid primary key,
  cas_ticket character varying,
  person     character varying not null,
  last_read  timestamptz       not null default now()
);

create table roles (
  session uuid references sessions (id) on delete cascade,
  role    character varying not null
);

create index roles_session_idx ON roles (session);
