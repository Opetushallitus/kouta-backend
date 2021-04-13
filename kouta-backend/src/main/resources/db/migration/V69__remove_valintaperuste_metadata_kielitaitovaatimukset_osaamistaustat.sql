-- Poistetaan valintaperusteiden metadatasta kielitaitovaatimukset sekä osaamistaustaKoodiUrit
UPDATE valintaperusteet
SET metadata = metadata #- '{kielitaitovaatimukset}';

UPDATE valintaperusteet
SET metadata = metadata #- '{osaamistaustaKoodiUrit}';
