create sequence discovery_certificate_id_seq start 1 increment 1;
create sequence discovery_id_seq start 1 increment 1;

create table discovery_certificate
(
    id              bigint not null,
    uuid            varchar not null,
    base64content   varchar null default null,
    discovery_id    bigint null default null,
    meta            text null default null,
    primary key (id)
);

create table discovery_history
(
    id      bigint not null,
    uuid    varchar not null,
    name    varchar not null,
    status  varchar not null,
    meta    text null default null,
    primary key (id)
);