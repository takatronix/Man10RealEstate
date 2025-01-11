
create table region
(
    id         int auto_increment
        primary key,
    server     varchar(16)  not null comment 'サーバー名',
    world      varchar(16)  not null comment 'ワールド名',
    owner_uuid varchar(36)  null comment '所有者のUUID nullならだれも所有していない',
    owner_name varchar(16)  null comment '所有者の名前',
    x          double       not null comment 'テレポートすると飛ぶ場所',
    y          double       not null comment 'テレポートすると飛ぶ場所',
    z          double       not null comment 'テレポートすると飛ぶ場所',
    pitch      double       not null comment 'テレポートすると飛ぶ場所',
    yaw        double       not null comment 'テレポートすると飛ぶ場所',
    sx         double       null comment '始点',
    sy         double       null comment '始点',
    sz         double       null comment '始点',
    ex         double       null comment '終点',
    ey         double       null comment '終点',
    ez         double       null comment '終点',
    name       varchar(128) null comment '名称',
    created    datetime     not null default now() comment '作成日',
    status     varchar(16)  not null DEFAULT 'OnSale',
    tax_status varchar(16)  not null DEFAULT 'SUCCESS',
    price      double       null comment '販売金額
OnSale状態になったときに販売価格
売り上げ金額は、売上テーブルに登録しオフラインでも売買できるとする
',
    profit     double       not null default 0.0 comment '土地の利益',
    span       Int          null comment '支払うスパン(0:moth 1:week 2:day)',
    data       varchar(256) null
);


CREATE TABLE `region_user` (

  `id` int NOT NULL auto_increment primary key,
  `region_id` int NOT NULL DEFAULT '0' COMMENT 'リージョンID',
  `player` varchar(16) DEFAULT NULL,
  `uuid` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '',
  `created_time` datetime NOT NULL COMMENT '登録日',
  `status` varchar(16) DEFAULT '' COMMENT 'ステータス',
  `is_rent` TINYINT(1) DEFAULT '0' COMMENT '1:賃貸 0:賃貸じゃない',
  `paid_date` datetime NOT NULL DEFAULT now() COMMENT '最後に支払った日',
  `allow_all` TINYINT NOT NULL DEFAULT '0' COMMENT '権限設定(1で許す 0で許さない)',
  `allow_block` TINYINT NOT NULL DEFAULT '0' COMMENT '権限設定(1で許す 0で許さない)',
  `allow_inv` TINYINT NOT NULL DEFAULT '0' COMMENT '権限設定(1で許す 0で許さない)',
  `allow_door` TINYINT NOT NULL DEFAULT '0' COMMENT '権限設定(1で許す 0で許さない)',
  `allow_item_frame` TINYINT NOT NULL DEFAULT '0' COMMENT '権限設定(1で許す 0で許さない)',
  `rent`    double DEFAULT '0.0' COMMENT '賃料',
  INDEX `region_id` (`region_id`),
  KEY `uuid` (`uuid`)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

create table bookmark
(
    id        int auto_increment
        primary key,
    player    varchar(16) null,
    uuid      varchar(36) null,
    region_id int         null
);

create index bookmark_uuid_index
    on bookmark (uuid);

create table log
(
    id        int auto_increment
        primary key,
    date      datetime default CURRENT_TIMESTAMP not null,
    player    varchar(16)                        not null,
    uuid      varchar(36)                        not null,
    region_id int                                null,
    log       varchar(256)                       null
);