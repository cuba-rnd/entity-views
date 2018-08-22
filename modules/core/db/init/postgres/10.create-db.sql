create table PLAYGROUND_SAMPLE_ENTITY (
    ID uuid not null,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    -- TODO add your columns here
    primary key (ID)
)^

-- begin PLAYGROUND_SAMPLE_ENTITY
create table PLAYGROUND_SAMPLE_ENTITY (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    NAME varchar(255) not null,
    USER_ID uuid,
    --
    primary key (ID)
)^
-- end PLAYGROUND_SAMPLE_ENTITY
