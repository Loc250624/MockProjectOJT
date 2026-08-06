-- Separate OAuth accounts by provider while preserving all Users rows and business data.
-- AuthProviderConverter stores provider values in lowercase.

-- Legacy rows without a provider predate OAuth and are local credential accounts.
UPDATE Users
SET auth_provider = 'local'
WHERE auth_provider IS NULL OR TRIM(auth_provider) = '';

-- Remove only cross-provider identity links created by the former email auto-link flow.
-- The owning user, profile, enrollments, courses and history are intentionally untouched.
DELETE upi
FROM user_provider_identities upi
JOIN Users u ON u.id = upi.user_id
WHERE LOWER(upi.provider) IN ('google', 'github')
  AND LOWER(upi.provider) <> LOWER(u.auth_provider);

-- Find and remove the global single-column unique index on Users.email.
SET @global_email_index := (
    SELECT s.INDEX_NAME
    FROM information_schema.statistics s
    WHERE s.table_schema = DATABASE()
      AND LOWER(s.table_name) = LOWER('Users')
      AND LOWER(s.column_name) = 'email'
      AND s.non_unique = 0
      AND s.index_name <> 'PRIMARY'
    GROUP BY s.INDEX_NAME
    HAVING COUNT(*) = 1
    LIMIT 1
);

SET @drop_global_email_index_sql := IF(
    @global_email_index IS NULL,
    'SELECT 1',
    CONCAT(
        'ALTER TABLE `Users` DROP INDEX `',
        REPLACE(@global_email_index, '`', '``'),
        '`'
    )
);

PREPARE drop_global_email_index_stmt FROM @drop_global_email_index_sql;
EXECUTE drop_global_email_index_stmt;
DEALLOCATE PREPARE drop_global_email_index_stmt;

-- Add provider-scoped email uniqueness only when it is not already present.
SET @provider_email_index_exists := (
    SELECT COUNT(*)
    FROM (
        SELECT s.INDEX_NAME
        FROM information_schema.statistics s
        WHERE s.table_schema = DATABASE()
          AND LOWER(s.table_name) = LOWER('Users')
          AND s.non_unique = 0
        GROUP BY s.INDEX_NAME
        HAVING GROUP_CONCAT(LOWER(s.column_name) ORDER BY s.seq_in_index)
               = 'auth_provider,email'
    ) provider_email_indexes
);

SET @add_provider_email_index_sql := IF(
    @provider_email_index_exists > 0,
    'SELECT 1',
    'ALTER TABLE `Users` ADD CONSTRAINT `uk_users_auth_provider_email` UNIQUE (`auth_provider`, `email`)'
);

PREPARE add_provider_email_index_stmt FROM @add_provider_email_index_sql;
EXECUTE add_provider_email_index_stmt;
DEALLOCATE PREPARE add_provider_email_index_stmt;

ALTER TABLE Users
MODIFY auth_provider VARCHAR(255) NOT NULL,
MODIFY email VARCHAR(255) NOT NULL;
