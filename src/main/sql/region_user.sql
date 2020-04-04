CREATE TABLE `region_user` (

  `region_id` int NOT NULL DEFAULT '0' COMMENT 'リージョンID',
  `type` int NOT NULL DEFAULT '0' COMMENT '0:共同所有者 1:使用者',
  `player` varchar(16) DEFAULT NULL,
  `uuid` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '',
  `created_time` datetime NOT NULL COMMENT '登録日',
  `status` varchar(16) DEFAULT '' COMMENT 'ステータス',
  `isRent` TINYINT(1) DEFAULT '0' COMMENT '1:賃貸 0:賃貸じゃない',
  `deposit` double NOT NULL DEFAULT '0' COMMENT '支払った額',
  `paid_date` datetime NOT NULL DEFAULT now() COMMENT '最後に支払った日',
  PRIMARY KEY (`region_id`),
  KEY `uuid` (`uuid`)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;