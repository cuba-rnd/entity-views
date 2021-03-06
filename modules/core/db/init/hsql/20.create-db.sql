-- begin PLAYGROUND_SAMPLE_ENTITY
alter table PLAYGROUND_SAMPLE_ENTITY add constraint FK_PLAYGROUND_SAMPLE_ENTITY_ON_PARENT foreign key (PARENT_ID) references PLAYGROUND_SAMPLE_ENTITY(ID)^
alter table PLAYGROUND_SAMPLE_ENTITY add constraint FK_PLAYGROUND_SAMPLE_ENTITY_ON_USER foreign key (USER_ID) references SEC_USER(ID)^
create index IDX_PLAYGROUND_SAMPLE_ENTITY_ON_PARENT on PLAYGROUND_SAMPLE_ENTITY (PARENT_ID)^
create index IDX_PLAYGROUND_SAMPLE_ENTITY_ON_USER on PLAYGROUND_SAMPLE_ENTITY (USER_ID)^
-- end PLAYGROUND_SAMPLE_ENTITY
-- begin PLAYGROUND_ENTITY_PARAMETER
alter table PLAYGROUND_ENTITY_PARAMETER add constraint FK_PLAYGROUND_ENTITY_PARAMETER_ON_SAMPLE_ENTITY foreign key (SAMPLE_ENTITY_ID) references PLAYGROUND_SAMPLE_ENTITY(ID)^
alter table PLAYGROUND_ENTITY_PARAMETER add constraint FK_PLAYGROUND_ENTITY_PARAMETER_ON_COMP_ENTITY foreign key (COMP_ENTITY_ID) references PLAYGROUND_SAMPLE_ENTITY(ID)^
create index IDX_PLAYGROUND_ENTITY_PARAMETER_ON_SAMPLE_ENTITY on PLAYGROUND_ENTITY_PARAMETER (SAMPLE_ENTITY_ID)^
create index IDX_PLAYGROUND_ENTITY_PARAMETER_ON_COMP_ENTITY on PLAYGROUND_ENTITY_PARAMETER (COMP_ENTITY_ID)^
-- end PLAYGROUND_ENTITY_PARAMETER
