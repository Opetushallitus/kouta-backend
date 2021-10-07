update toteutukset
set metadata = jsonb_set(metadata, '{jarjestetaanErityisopetuksena}', to_jsonb(metadata -> 'tuvaErityisopetuksena'), TRUE)
where metadata ->> 'tyyppi' = 'tuva' and (metadata -> 'tuvaErityisopetuksena') is not null;

update toteutukset
set metadata = metadata #- '{tuvaErityisopetuksena}';
