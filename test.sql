create table test_minio.media_files
(
    id        varchar(32)   not null comment '主键'
        primary key,
    filename  varchar(255)  null comment '文件名称',
    file_type varchar(12)   null comment '文件类型 （图片、文档、视频）',
    file_id   varchar(32)   null comment '文件id',
    file_path varchar(512)  null comment '文件存储目录',
    url       varchar(1024) null comment '文件访问地址',
    file_size bigint        null comment '文件大小'
)
    comment '文件信息表';

