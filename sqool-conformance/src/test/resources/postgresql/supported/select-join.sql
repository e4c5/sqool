-- SELECT with JOIN (normalized to SelectStatement)
SELECT u.id, u.name, o.total
FROM users u
INNER JOIN orders o ON u.id = o.user_id;

SELECT u.id, u.name
FROM users u
LEFT JOIN orders o ON u.id = o.user_id
WHERE o.total > 100;

SELECT u.id, o.total
FROM users u
JOIN orders o ON u.id = o.user_id
ORDER BY o.total DESC
LIMIT 10;
