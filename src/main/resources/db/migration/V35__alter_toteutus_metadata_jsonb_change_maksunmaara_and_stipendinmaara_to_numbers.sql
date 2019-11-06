-- toteutus.metadata.opetus.maksunMaara and stipendinMaara are now numbers, not Kielistetty objects


--- "maksunMaara": {"en": "200", "fi": "100", "sv": "300"}
--> "maksunMaara": 100
UPDATE toteutukset set metadata = jsonb_set(metadata,
                                            '{opetus, maksunMaara}',
                                            to_jsonb((metadata->'opetus'->'maksunMaara'->>'fi')::int))
WHERE metadata->'opetus'->'maksunMaara'->'fi' IS NOT NULL;

--- "maksunMaara": {}
--> "maksunMaara": null
UPDATE toteutukset set metadata = jsonb_set(metadata,
                                            '{opetus, maksunMaara}',
                                            'null')
WHERE metadata->'opetus'->'maksunMaara' = '{}';

--- "stipendinMaara": {"en": "200", "fi": "100", "sv": "300"}
--> "stipendinMaara": 100
UPDATE toteutukset set metadata = jsonb_set(metadata,
                                            '{opetus, stipendinMaara}',
                                            to_jsonb((metadata->'opetus'->'stipendinMaara'->>'fi')::int))
WHERE metadata->'opetus'->'stipendinMaara'->'fi' IS NOT NULL;

--- "stipendinMaara": {}
--> "stipendinMaara": null
UPDATE toteutukset set metadata = jsonb_set(metadata,
                                            '{opetus, stipendinMaara}',
                                            'null')
WHERE metadata->'opetus'->'stipendinMaara' = '{}';
