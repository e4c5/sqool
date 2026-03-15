-- INNER JOIN valid across all four dialects (MySQL, SQLite, PostgreSQL, Oracle)
SELECT u.id, u.name, o.total FROM users u INNER JOIN orders o ON u.id = o.user_id;
