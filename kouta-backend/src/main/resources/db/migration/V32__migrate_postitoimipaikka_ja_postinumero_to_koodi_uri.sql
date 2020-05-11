update hakukohteet
  set liitteiden_toimitusosoite = liitteiden_toimitusosoite #- '{osoite,postitoimipaikka}';
update hakukohteet
  set liitteiden_toimitusosoite = jsonb_set(liitteiden_toimitusosoite, '{osoite,postinumeroKoodiUri}',
      to_jsonb(concat('posti_', liitteiden_toimitusosoite->'osoite'->>'postinumero', '#2')), true)
  where liitteiden_toimitusosoite->'osoite'->>'postinumero' is not null;
update hakukohteet
  set liitteiden_toimitusosoite = liitteiden_toimitusosoite #- '{osoite,postinumero}';

update hakukohteiden_liitteet
  set toimitusosoite = toimitusosoite #- '{osoite,postitoimipaikka}';
update hakukohteiden_liitteet
  set toimitusosoite = jsonb_set(toimitusosoite, '{osoite,postinumeroKoodiUri}',
    to_jsonb(concat('posti_', toimitusosoite->'osoite'->>'postinumero', '#2')), true)
  where toimitusosoite->'osoite'->>'postinumero' is not null;
update hakukohteiden_liitteet
  set toimitusosoite = toimitusosoite #- '{osoite,postinumero}';

DO
$BODY$
    declare
        result record;
        t jsonb;
        new_array jsonb;
    BEGIN
        FOR result IN SELECT id, tilaisuudet FROM hakukohteiden_valintakokeet where tilaisuudet is not null
            LOOP
                new_array = '[]'::JSONB;
                FOR t in select * from jsonb_array_elements(result.tilaisuudet)
                    loop
                        t = t #- '{osoite, postitoimipaikka}';
                        t = jsonb_set(t, '{osoite,postinumeroKoodiUri}', to_jsonb(concat('posti_', t->'osoite'->>'postinumero', '#2')), true);
                        t = t #- '{osoite, postinumero}';
                        new_array = new_array || t;
                    end loop;
                update hakukohteiden_valintakokeet set tilaisuudet = new_array where id = result.id;
            END LOOP;
    END;
$BODY$ language plpgsql;

DO
$BODY$
    declare
        result record;
        t jsonb;
        new_array jsonb;
    BEGIN
        FOR result IN SELECT id, tilaisuudet FROM hakujen_valintakokeet where tilaisuudet is not null
            LOOP
                new_array = '[]'::JSONB;
                FOR t in select * from jsonb_array_elements(result.tilaisuudet)
                    loop
                        t = t #- '{osoite, postitoimipaikka}';
                        t = jsonb_set(t, '{osoite,postinumeroKoodiUri}', to_jsonb(concat('posti_', t->'osoite'->>'postinumero', '#2')), true);
                        t = t #- '{osoite, postinumero}';
                        new_array = new_array || t;
                    end loop;
                update hakujen_valintakokeet set tilaisuudet = new_array where id = result.id;
            END LOOP;
    END;
$BODY$ language plpgsql;