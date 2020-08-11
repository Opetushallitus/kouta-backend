update haut set hakulomake = '{"fi": "' || hakulomake || '"}' where hakulomake is not null;
alter table haut alter column hakulomake set data type jsonb using hakulomake::jsonb;

update hakukohteet set hakulomake = '{"fi": "' || hakulomake || '"}' where hakulomake is not null;
alter table hakukohteet alter column hakulomake set data type jsonb using hakulomake::jsonb;

update haut_history set hakulomake = '{"fi": "' || hakulomake || '"}' where hakulomake is not null;
alter table haut_history alter column hakulomake set data type jsonb using hakulomake::jsonb;

update hakukohteet_history set hakulomake = '{"fi": "' || hakulomake || '"}' where hakulomake is not null;
alter table hakukohteet_history alter column hakulomake set data type jsonb using hakulomake::jsonb;