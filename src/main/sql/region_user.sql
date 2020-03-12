create table Man10RealEstate.region_user
(
    region_id    int           not null comment 'リージョンID: プライマリキー',
    type         int default 0 null comment 'タイプ: 0 共同所有者(default) 1:使用者 ',
    uuid         varchar(36)   not null,
    name         varchar(16)   not null comment 'ユーザー名',
    created_time datetime      not null comment '登録日時',
    status       varchar(16)   null comment 'ステータス: "Lock"　など、現在状態',
    deposit      double default 0.0 not null comment '支払った額',
    paid_date    datetime      null comment '最後に支払った日'

    primary key (region_id, uuid)
);

create index region_user_uuid_index
    on Man10RealEstate.region_user (uuid);