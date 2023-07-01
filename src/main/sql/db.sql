create table city
(
	id int auto_increment,
	name varchar(16) not null,
	x double default 0.0 null,
	y double default 0.0 null,
	z double default 0.0 null,
	pitch float default 0.0 null,
	yaw float default 0.0 null,
	server varchar(16) null,
	world varchar(16) null,
	sx int default 0 null,
	sy int default 0 null,
	sz int default 0 null,
	ex int default 0 null,
	ey int default 0 null,
	ez int default 0 null,
	tax double default 0.0 null,
	max_user int default 0 null,
	buy_score int default 0 null,
	live_score int default 0 null,
	default_price double default 0 null,
	constraint city_pk
		primary key (id)
);

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


CREATE TABLE `liked_index` (
  `region_id` int unsigned NOT NULL COMMENT 'いいねしたリージョンのデータ',
  `player` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `uuid` varchar(36) DEFAULT NULL,
  `score` double NOT NULL DEFAULT '0' COMMENT 'スコア',
  `is_like` tinyint(1) NOT NULL DEFAULT '1' COMMENT '0:いいね解除 1:いいね',
  `date` DATETIME NOT NULL DEFAULT now(),
  KEY `uuid` (`uuid`),
  KEY `region_id` (`region_id`)
)

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
  `rent`    double DEFAULT '0.0' COMMENT '賃料',
  INDEX `region_id` (`region_id`),
  KEY `uuid` (`uuid`)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;