-- VERIFICATION QUERIES TEMPLATE
-- KHÔNG CHẠY TRÊN PRODUCTION.
-- Codex phải thay tên bảng/cột dựa trên entity và migration thực tế.
-- Không dùng DROP/TRUNCATE/DELETE.

-- 1. Inventory table/column (MySQL)
SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND (
    LOWER(TABLE_NAME) LIKE '%payment%'
    OR LOWER(TABLE_NAME) LIKE '%order%'
    OR LOWER(TABLE_NAME) LIKE '%enroll%'
    OR LOWER(TABLE_NAME) LIKE '%progress%'
    OR LOWER(TABLE_NAME) LIKE '%setting%'
    OR LOWER(TABLE_NAME) LIKE '%user%'
  )
ORDER BY TABLE_NAME, ORDINAL_POSITION;

-- 2. Inventory possible status values
-- SELECT status, COUNT(*) FROM <payment_table> GROUP BY status;
-- SELECT status, COUNT(*) FROM <order_table> GROUP BY status;

-- 3. Detect duplicate multiplication from joins
-- Compare raw successful transaction total with the application aggregate.
-- SELECT COUNT(*), SUM(amount) FROM <payment_table> WHERE status = '<SUCCESS>';

-- 4. Teacher ownership check
-- SELECT teacher_id, COUNT(DISTINCT course_id)
-- FROM <course_table>
-- GROUP BY teacher_id;

-- 5. Revenue by teacher
-- SELECT c.teacher_id, SUM(<recognized_amount>)
-- FROM <payment/order joins>
-- WHERE <recognized_status_condition>
-- GROUP BY c.teacher_id;

-- 6. New students
-- SELECT DATE(created_at), COUNT(DISTINCT id)
-- FROM <user_table>
-- WHERE <student_role_condition>
-- GROUP BY DATE(created_at);

-- 7. Active students
-- Replace with real activity source.
-- SELECT DATE(activity_at), COUNT(DISTINCT student_id)
-- FROM <activity_source>
-- GROUP BY DATE(activity_at);

-- 8. Settings inventory
-- SELECT setting_key, value_type, editable, sensitive, updated_at
-- FROM <settings_table>
-- ORDER BY category, setting_key;
