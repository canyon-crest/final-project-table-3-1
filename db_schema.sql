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

-- Comments on a thread. parentCommentID NULL = top-level; otherwise reply to another comment.
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

-- Seed categories so the app can list sections before you add any UI data.
INSERT INTO `category` (`name`, `description`, `sortOrder`) VALUES
  ('General', 'Open discussion', 10),
  ('Questions', 'Ask for help', 20),
  ('Announcements', 'Read-only style posts from moderators if you add roles later', 30)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);
