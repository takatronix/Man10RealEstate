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