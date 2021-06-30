update toteutukset
set metadata = jsonb_set(metadata, '{kielivalikoima, A1Kielet}', '[]', TRUE)
where metadata -> 'kielivalikoima' -> 'A1Kielet' is null;

update toteutukset
set metadata = jsonb_set(metadata, '{kielivalikoima, A2Kielet}', '[]', TRUE)
where metadata -> 'kielivalikoima' -> 'A2Kielet' is null;

update toteutukset
set metadata = jsonb_set(metadata, '{kielivalikoima, A1Kielet}', to_jsonb(metadata -> 'kielivalikoima' -> 'A1JaA2Kielet'), TRUE)
where metadata -> 'kielivalikoima' -> 'A1JaA2Kielet' is not null;

update toteutukset
set metadata = jsonb_set(metadata, '{kielivalikoima, A2Kielet}', to_jsonb(metadata -> 'kielivalikoima' -> 'A1JaA2Kielet'), TRUE)
where metadata -> 'kielivalikoima' -> 'A1JaA2Kielet' is not null;

update toteutukset
set metadata = metadata #- '{kielivalikoima, A1JaA2Kielet}'
where metadata -> 'kielivalikoima' -> 'A1JaA2Kielet' is not null;