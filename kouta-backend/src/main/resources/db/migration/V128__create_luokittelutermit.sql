create table luokittelutermit (
    luokittelutermi varchar not null,
    kieli kieli not null,
    primary key (luokittelutermi, kieli)
);