create table if not exists ddtd_example_task
(
    id          int auto_increment primary key,
    tenant      varchar(6)                          null comment 'tenant',
    task_id     varchar(16)                         null comment 'task unique id',
    task_data   clob                                null comment 'task data',
    state       tinyint                             null comment 'task state',
    inst_info   clob                                null comment 'instance(jvm) info',
    start_at    timestamp                           null comment 'start time of task',
    elapsed     varchar(20)                         null comment 'elapsed time of task',
    yn          smallint  default 1                 null comment 'logic delete flag',
    update_id   varchar(20)                         null comment 'last modified user id',
    update_name varchar(20)                         null comment 'last modified username',
    modified    timestamp default CURRENT_TIMESTAMP null comment 'last modified time',
    create_id   varchar(20)                         null comment 'creator id',
    create_name varchar(20)                         null comment 'creator name',
    created     timestamp default CURRENT_TIMESTAMP not null comment 'created time'
);

create index index_task_id
    on ddtd_example_task (task_id);