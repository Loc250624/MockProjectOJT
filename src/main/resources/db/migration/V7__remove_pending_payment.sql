-- Update legacy pending orders to paid if they have a successful transaction
UPDATE Orders o
JOIN Transactions t ON t.order_id = o.id
SET o.status = 'paid',
    o.paid_amount = o.total_amount
WHERE o.status = 'pending'
  AND t.status = 'success';

-- Update legacy pending orders to paid if transaction webhook response indicates success
UPDATE Orders o
JOIN Transactions t ON t.order_id = o.id
SET o.status = 'paid'
WHERE o.status = 'pending'
  AND (
    t.webhook_response LIKE '%vnp_ResponseCode=00%'
    OR t.webhook_response LIKE '%"vnp_ResponseCode":"00"%'
    OR t.webhook_response LIKE '%resultCode=0%'
    OR t.webhook_response LIKE '%"resultCode":0%'
  );

-- For all remaining pending orders, set status to failed
UPDATE Orders
SET status = 'failed'
WHERE status = 'pending';

-- For all remaining pending transactions, set status to failed
UPDATE Transactions
SET status = 'failed'
WHERE status = 'pending';

-- Insert missing enrollments for paid orders
INSERT INTO Course_Enrollments (student_id, course_id, progress_percentage, is_completed, enrolled_at)
SELECT DISTINCT o.user_id, oi.course_id, 0.00, 0, NOW()
FROM Orders o
JOIN Order_Items oi ON oi.order_id = o.id
WHERE o.status = 'paid'
  AND NOT EXISTS (
    SELECT 1 FROM Course_Enrollments ce
    WHERE ce.student_id = o.user_id AND ce.course_id = oi.course_id
  );
