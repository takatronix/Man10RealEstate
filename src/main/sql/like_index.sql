CREATE TABLE `liked_index` (
  `region_id` int unsigned NOT NULL COMMENT 'いいねしたリージョンのデータ',
  `player` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `uuid` varchar(36) DEFAULT NULL,
  `score` double NOT NULL DEFAULT '0' COMMENT 'スコア',
  `is_like` tinyint(1) NOT NULL DEFAULT '1' COMMENT '0:いいね解除 1:いいね',
  KEY `uuid` (`uuid`),
  KEY `region_id` (`region_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;