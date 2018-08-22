alter table PLAYGROUND_SAMPLE_ENTITY add column NAME varchar(255) ^
update PLAYGROUND_SAMPLE_ENTITY set NAME = '' where NAME is null ;
alter table PLAYGROUND_SAMPLE_ENTITY alter column NAME set not null ;
alter table PLAYGROUND_SAMPLE_ENTITY add column USER_ID uuid ;
