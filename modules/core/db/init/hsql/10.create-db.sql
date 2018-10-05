-- begin PLAYGROUND_SAMPLE_ENTITY
create table PLAYGROUND_SAMPLE_ENTITY (
    ID varchar(36) not null,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    NAME varchar(255) not null,
    PARENT_ID varchar(36),
    USER_ID varchar(36),
    --
    primary key (ID)
)^
-- end PLAYGROUND_SAMPLE_ENTITY
-- begin PLAYGROUND_ENTITY_PARAMETER
create table PLAYGROUND_ENTITY_PARAMETER (
    ID varchar(36) not null,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    NAME varchar(255),
    PARAM_VALUE bigint,
    SAMPLE_ENTITY_ID varchar(36),
    COMP_ENTITY_ID varchar(36),
    --
    primary key (ID)
)^
-- end PLAYGROUND_ENTITY_PARAMETER
-- begin SEC_USER
alter table SEC_USER add column LONG_NAME varchar(255) ^
alter table SEC_USER add column DTYPE varchar(100) ^
update SEC_USER set DTYPE = 'playground$ExtendedUser' where DTYPE is null ^
-- end SEC_USER
