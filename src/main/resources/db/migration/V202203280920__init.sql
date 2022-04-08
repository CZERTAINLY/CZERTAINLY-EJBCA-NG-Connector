create sequence authority_instance_id_seq start 1 increment 1;

create table authority_instance
(
    id                 int8         not null,
    i_cre              timestamp    not null,
    i_upd              timestamp    not null,
    uuid               varchar(255) not null,
    name               varchar(255),
    url                varchar(255),
    credential_uuid    varchar(255),
    credential_data    text,
    attributes         text,
    primary key (id)
);