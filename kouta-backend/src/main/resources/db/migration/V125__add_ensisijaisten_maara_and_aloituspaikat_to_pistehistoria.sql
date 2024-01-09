alter table pistehistoria
    add column if not exists aloituspaikat numeric;

COMMENT ON COLUMN pistehistoria.aloituspaikat IS 'Hakukohteen aloituspaikkojen määrä.';

alter table pistehistoria
    add column if not exists ensisijaisesti_hakeneet numeric;

COMMENT ON COLUMN pistehistoria.ensisijaisesti_hakeneet IS 'Ensisijaisesti hakeneiden määrä hakukohteella.';

alter table pistehistoria alter column pisteet drop not null;
