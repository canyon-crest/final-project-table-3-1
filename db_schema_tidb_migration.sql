-- One-time upgrade for TiDB when you already have the original 3 tables:
--   user, threads, comments (no category table, no categoryID / dateUpdated / parentCommentID).
-- Run against the same database you use in JDBC (example below).
-- If any step errors with "duplicate column", skip that step — it is already applied.
--
-- TiDB Cloud SQL Editor: Ctrl+Enter runs only the current/selected statement(s).
-- To run this whole file in order, use Shift+Ctrl+Enter (Windows/Linux), or select
-- all lines (Ctrl+A) and click Run — see:
-- https://docs.pingcap.com/tidbcloud/explore-data-with-chat2query

USE `cca_forum_db`;

-- 1) Categories (new table)
CREATE TABLE IF NOT EXISTS `category` (
  `categoryID` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(80) NOT NULL,
  `description` VARCHAR(255) NULL,
  `sortOrder` INT NOT NULL DEFAULT 0 COMMENT 'lower sorts first in UI lists',
  PRIMARY KEY (`categoryID`),
  UNIQUE KEY `uk_category_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- At least one row so existing threads can point at a category (adjust ID in UPDATE if needed).
INSERT INTO `category` (`name`, `description`, `sortOrder`) VALUES
  ('General', 'Open discussion', 10),
  ('Questions', 'Ask for help', 20),
  ('Announcements', 'Course or club announcements', 30)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);

-- 2) Threads: category + last-edited timestamp
-- categoryID defaults to 1 ("General" after the INSERT above). Change default later if you want.
ALTER TABLE `threads`
  ADD COLUMN `categoryID` INT UNSIGNED NOT NULL DEFAULT 1 AFTER `threadID`,
  ADD COLUMN `dateUpdated` DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT 'NULL if never edited',
  ADD KEY `idx_threads_category` (`categoryID`),
  ADD KEY `idx_threads_created` (`dateCreated`);

-- Optional: TiDB supports foreign keys on recent versions; comment out if your cluster rejects this.
ALTER TABLE `threads`
  ADD CONSTRAINT `fk_threads_category`
    FOREIGN KEY (`categoryID`) REFERENCES `category` (`categoryID`)
    ON DELETE RESTRICT ON UPDATE CASCADE;

-- 3) Comments: add reply threading support.
-- parentCommentID design:
--   - NULL = top-level comment
--   - non-NULL = reply to another comment row
-- This allows recursive nesting (a reply can itself be replied to, repeatedly).
-- (threadID / authorID usually already indexed from your existing foreign keys.)
ALTER TABLE `comments`
  ADD COLUMN `parentCommentID` INT UNSIGNED NULL AFTER `authorID`,
  ADD KEY `idx_comments_parent` (`parentCommentID`);

ALTER TABLE `comments`
  ADD CONSTRAINT `fk_comment_parent`
    FOREIGN KEY (`parentCommentID`) REFERENCES `comments` (`commentID`)
    ON DELETE CASCADE ON UPDATE CASCADE;

-- 4) Avatar part catalogs (for layered profile sprite customization)
CREATE TABLE IF NOT EXISTS `avatar_headpiece` (
  `headpiece_id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `code` VARCHAR(50) NOT NULL,
  `display_name` VARCHAR(80) NOT NULL,
  `asset_key` VARCHAR(120) NOT NULL,
  `unlock_level` INT NOT NULL DEFAULT 0,
  `is_active` TINYINT(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`headpiece_id`),
  UNIQUE KEY `uk_avatar_headpiece_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `avatar_clothing` (
  `clothing_id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `code` VARCHAR(50) NOT NULL,
  `display_name` VARCHAR(80) NOT NULL,
  `asset_key` VARCHAR(120) NOT NULL,
  `unlock_level` INT NOT NULL DEFAULT 0,
  `is_active` TINYINT(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`clothing_id`),
  UNIQUE KEY `uk_avatar_clothing_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `avatar_accessory` (
  `accessory_id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `code` VARCHAR(50) NOT NULL,
  `display_name` VARCHAR(80) NOT NULL,
  `asset_key` VARCHAR(120) NOT NULL,
  `unlock_level` INT NOT NULL DEFAULT 0,
  `is_active` TINYINT(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`accessory_id`),
  UNIQUE KEY `uk_avatar_accessory_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `avatar_headpiece` (`code`, `display_name`, `asset_key`, `unlock_level`, `is_active`) VALUES
  ('none', 'None', 'headpiece:none', 0, 1),
  ('cap_red', 'Red Cap', 'headpiece:cap_red', 0, 1),
  ('beanie_gray', 'Gray Beanie', 'headpiece:beanie_gray', 1, 1),
  ('visor_white', 'White Visor', 'headpiece:visor_white', 2, 1),
  ('headband_raven', 'Raven Headband', 'headpiece:headband_raven', 3, 1),
  ('crown_gold', 'Gold Crown', 'headpiece:crown_gold', 6, 1),
  ('halo', 'Halo', 'headpiece:halo', 5, 1)
ON DUPLICATE KEY UPDATE `display_name` = VALUES(`display_name`);

INSERT INTO `avatar_clothing` (`code`, `display_name`, `asset_key`, `unlock_level`, `is_active`) VALUES
  ('default_tee', 'Default Tee', 'clothing:default_tee', 0, 1),
  ('polo_navy', 'Navy Polo', 'clothing:polo_navy', 0, 1),
  ('raven_tee', 'Raven Tee', 'clothing:raven_tee', 1, 1),
  ('hoodie_black', 'Black Hoodie', 'clothing:hoodie_black', 2, 1),
  ('sweater_cream', 'Cream Sweater', 'clothing:sweater_cream', 2, 1),
  ('dress_shirt', 'Dress Shirt', 'clothing:dress_shirt', 3, 1),
  ('letterman', 'Letterman Jacket', 'clothing:letterman', 4, 1),
  ('jacket_red', 'Red Jacket', 'clothing:jacket_red', 5, 1),
  ('tracksuit', 'Tracksuit', 'clothing:tracksuit', 3, 1),
  ('formal_gown', 'Formal Gown', 'clothing:formal_gown', 6, 1),
  ('team_jersey', 'Team Jersey', 'clothing:team_jersey', 4, 1)
ON DUPLICATE KEY UPDATE `display_name` = VALUES(`display_name`);

INSERT INTO `avatar_accessory` (`code`, `display_name`, `asset_key`, `unlock_level`, `is_active`) VALUES
  ('none', 'None', 'accessory:none', 0, 1),
  ('glasses', 'Glasses', 'accessory:glasses', 1, 1),
  ('scarf_striped', 'Striped Scarf', 'accessory:scarf_striped', 2, 1),
  ('watch_silver', 'Silver Watch', 'accessory:watch_silver', 2, 1),
  ('bow_tie', 'Bow Tie', 'accessory:bow_tie', 3, 1),
  ('star_pin', 'Star Pin', 'accessory:star_pin', 3, 1),
  ('backpack', 'Backpack', 'accessory:backpack', 4, 1),
  ('earbuds', 'Earbuds', 'accessory:earbuds', 1, 1)
ON DUPLICATE KEY UPDATE `display_name` = VALUES(`display_name`);

-- 5) Add per-user selected avatar-part IDs.
ALTER TABLE `user`
  ADD COLUMN `avatar_headpiece_id` INT UNSIGNED NULL,
  ADD COLUMN `avatar_clothing_id` INT UNSIGNED NULL,
  ADD COLUMN `avatar_accessory_id` INT UNSIGNED NULL,
  ADD KEY `idx_user_avatar_headpiece` (`avatar_headpiece_id`),
  ADD KEY `idx_user_avatar_clothing` (`avatar_clothing_id`),
  ADD KEY `idx_user_avatar_accessory` (`avatar_accessory_id`);

ALTER TABLE `user`
  ADD CONSTRAINT `fk_user_avatar_headpiece`
    FOREIGN KEY (`avatar_headpiece_id`) REFERENCES `avatar_headpiece` (`headpiece_id`)
    ON DELETE SET NULL ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_user_avatar_clothing`
    FOREIGN KEY (`avatar_clothing_id`) REFERENCES `avatar_clothing` (`clothing_id`)
    ON DELETE SET NULL ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_user_avatar_accessory`
    FOREIGN KEY (`avatar_accessory_id`) REFERENCES `avatar_accessory` (`accessory_id`)
    ON DELETE SET NULL ON UPDATE CASCADE;

-- 6) Gamification columns on users.
ALTER TABLE `user`
  ADD COLUMN `xp_total` INT NOT NULL DEFAULT 0,
  ADD COLUMN `level` INT NOT NULL DEFAULT 1;

UPDATE `user`
SET `xp_total` = IFNULL(`xp_total`, 0),
    `level` = GREATEST(1, FLOOR(IFNULL(`xp_total`, 0) / 100) + 1);

-- 7) Like/dislike tables.
CREATE TABLE IF NOT EXISTS `thread_reaction` (
  `threadID` INT UNSIGNED NOT NULL,
  `userID` INT UNSIGNED NOT NULL,
  `reaction` TINYINT NOT NULL COMMENT '1 = like, -1 = dislike',
  `dateCreated` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateUpdated` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`threadID`, `userID`),
  KEY `idx_thread_reaction_user` (`userID`),
  CONSTRAINT `fk_thread_reaction_thread`
    FOREIGN KEY (`threadID`) REFERENCES `threads` (`threadID`)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_thread_reaction_user`
    FOREIGN KEY (`userID`) REFERENCES `user` (`userID`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `comment_reaction` (
  `commentID` INT UNSIGNED NOT NULL,
  `userID` INT UNSIGNED NOT NULL,
  `reaction` TINYINT NOT NULL COMMENT '1 = like, -1 = dislike',
  `dateCreated` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateUpdated` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`commentID`, `userID`),
  KEY `idx_comment_reaction_user` (`userID`),
  CONSTRAINT `fk_comment_reaction_comment`
    FOREIGN KEY (`commentID`) REFERENCES `comments` (`commentID`)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_comment_reaction_user`
    FOREIGN KEY (`userID`) REFERENCES `user` (`userID`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
