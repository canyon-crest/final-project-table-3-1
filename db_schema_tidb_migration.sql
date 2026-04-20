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

-- 3) Comments: optional reply threading
-- (threadID / authorID usually already indexed from your existing foreign keys.)
ALTER TABLE `comments`
  ADD COLUMN `parentCommentID` INT UNSIGNED NULL AFTER `authorID`,
  ADD KEY `idx_comments_parent` (`parentCommentID`);

ALTER TABLE `comments`
  ADD CONSTRAINT `fk_comment_parent`
    FOREIGN KEY (`parentCommentID`) REFERENCES `comments` (`commentID`)
    ON DELETE CASCADE ON UPDATE CASCADE;
