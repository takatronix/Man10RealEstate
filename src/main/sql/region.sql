create table man10builder.region
(
    id       int auto_increment
        primary key,
    server   varchar(16)  not null comment 'サーバー名',
    world    varchar(16)  not null comment 'ワールド名',
    x        double       not null comment 'テレポートすると飛ぶ場所',
    y        double       not null comment 'テレポートすると飛ぶ場所',
    z        double       not null comment 'テレポートすると飛ぶ場所',
    pitch    double       not null comment 'テレポートすると飛ぶ場所',
    yaw      double       not null comment 'テレポートすると飛ぶ場所',
    sx       double       null comment '始点',
    sy       double       null comment '始点',
    sz       double       null comment '始点',
    ex       double       null comment '終点',
    ey       double       null comment '終点',
    ez       double       null comment '終点',
    enabled  tinyint(1)   not null comment '有効か無効かフラグ
無効になっている場合、評価などは一切できない',
    protect  tinyint(1)   null comment '保護フラグ
保護がかかっている場合、regeon_authテーブルに定義されている権限者もしくはred.man10.region.auth権限をもっているもの以外はブロック破壊できない',
    name     varchar(128) null comment '名称',
    datetime datetime     null comment '作成日'
)
    comment '領域管理のためのテーブル';

