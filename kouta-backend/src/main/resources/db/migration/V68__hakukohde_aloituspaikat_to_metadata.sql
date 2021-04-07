update hakukohteet
set metadata = jsonb_set(metadata, '{aloituspaikat}', '{}', TRUE)
where metadata -> 'aloituspaikat' is null;

update hakukohteet
set metadata = jsonb_set(metadata, '{aloituspaikat, lukumaara}', to_jsonb(aloituspaikat), TRUE)
where aloituspaikat is not null and metadata -> 'aloituspaikat' ->> 'lukumaara' is null;

update hakukohteet
set metadata = jsonb_set(metadata, '{aloituspaikat, ensikertalaisille}', to_jsonb(ensikertalaisen_aloituspaikat), TRUE)
where ensikertalaisen_aloituspaikat is not null and metadata -> 'aloituspaikat' ->> 'ensikertalaisille' is null;