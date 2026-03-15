-- Simple SELECT valid across all four dialects (MySQL, SQLite, PostgreSQL, Oracle)
SELECT id, name FROM users;
SELECT * FROM products;
SELECT id AS user_id, name AS full_name FROM users;
SELECT DISTINCT status FROM orders;
