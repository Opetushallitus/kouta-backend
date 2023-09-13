alter table pistehistoria
    add column if not exists valintatapajono_tyyppi varchar;

COMMENT ON COLUMN pistehistoria.valintatapajono_tyyppi IS 'Hakukohteen valintatapajonon tyyppin koodiuri.';
