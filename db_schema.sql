-- Forum schema: full create for a new database (MySQL 8+ / TiDB).
-- If you already created only user + threads + comments in TiDB (older 3-table version),
-- do not re-run the CREATEs here; run `db_schema_tidb_migration.sql` once instead.
-- Java/Swing connects with JDBC to the same database name.

CREATE DATABASE IF NOT EXISTS `cca_forum_db`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE `cca_forum_db`;

-- Registered users (password hash from bcrypt or similar in the Java app).
CREATE TABLE IF NOT EXISTS `user` (
  `userID` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(50) NOT NULL,
  `password` VARCHAR(255) NOT NULL COMMENT 'bcrypt or other strong hash',
  `dateCreated` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`userID`),
  UNIQUE KEY `uk_user_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Forum sections (e.g. "General", "Homework Help").
CREATE TABLE IF NOT EXISTS `category` (
  `categoryID` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(80) NOT NULL,
  `description` VARCHAR(255) NULL,
  `sortOrder` INT NOT NULL DEFAULT 0 COMMENT 'lower sorts first in UI lists',
  PRIMARY KEY (`categoryID`),
  UNIQUE KEY `uk_category_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Discussion threads; each belongs to one category and one author.
CREATE TABLE IF NOT EXISTS `threads` (
  `threadID` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `categoryID` INT UNSIGNED NOT NULL,
  `authorID` INT UNSIGNED NOT NULL,
  `title` VARCHAR(150) NOT NULL,
  `content` TEXT NOT NULL,
  `dateCreated` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateUpdated` DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT 'NULL if never edited',
  PRIMARY KEY (`threadID`),
  KEY `idx_threads_category` (`categoryID`),
  KEY `idx_threads_author` (`authorID`),
  KEY `idx_threads_created` (`dateCreated`),
  CONSTRAINT `fk_threads_category`
    FOREIGN KEY (`categoryID`) REFERENCES `category` (`categoryID`)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_threads_author`
    FOREIGN KEY (`authorID`) REFERENCES `user` (`userID`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Comments on a thread.
-- Reply threading model:
--   - parentCommentID = NULL   -> top-level comment
--   - parentCommentID = <id>   -> reply to that comment
-- Because parentCommentID references comments.commentID, replies can nest recursively
-- (reply-to-reply-to-reply, etc.) with no fixed depth limit at the schema level.
CREATE TABLE IF NOT EXISTS `comments` (
  `commentID` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `threadID` INT UNSIGNED NOT NULL,
  `authorID` INT UNSIGNED NOT NULL,
  `parentCommentID` INT UNSIGNED NULL,
  `content` TEXT NOT NULL,
  `dateCreated` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`commentID`),
  KEY `idx_comments_thread` (`threadID`),
  KEY `idx_comments_author` (`authorID`),
  KEY `idx_comments_parent` (`parentCommentID`),
  CONSTRAINT `fk_comment_thread`
    FOREIGN KEY (`threadID`) REFERENCES `threads` (`threadID`)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_comment_author`
    FOREIGN KEY (`authorID`) REFERENCES `user` (`userID`)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_comment_parent`
    FOREIGN KEY (`parentCommentID`) REFERENCES `comments` (`commentID`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Avatar part catalogs (preset IDs for layered sprite customization).
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

-- Add avatar selections to users (nullable so legacy rows still load).
ALTER TABLE `user`
  ADD COLUMN IF NOT EXISTS `avatar_headpiece_id` INT UNSIGNED NULL,
  ADD COLUMN IF NOT EXISTS `avatar_clothing_id` INT UNSIGNED NULL,
  ADD COLUMN IF NOT EXISTS `avatar_accessory_id` INT UNSIGNED NULL,
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

-- Seed categories so the app can list sections before you add any UI data.
INSERT INTO `category` (`name`, `description`, `sortOrder`) VALUES
  ('General', 'Open discussion', 10),
  ('Questions', 'Ask for help', 20),
  ('Announcements', 'Read-only style posts from moderators if you add roles later', 30)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);

-- Seed avatar options for initial customization support.
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
