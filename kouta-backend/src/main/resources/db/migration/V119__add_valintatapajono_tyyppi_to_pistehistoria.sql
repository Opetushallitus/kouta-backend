alter table pistehistoria
    add column valintatapajono_tyyppi varchar;

COMMENT ON COLUMN pistehistoria.valintatapajono_tyyppi IS 'Hakukohteen valintatapajonon tyyppin koodiuri.';
