-- Basic SELECT statements for PostgreSQL conformance
SELECT 1;
SELECT 'hello';
SELECT true;
SELECT false;
SELECT null;
SELECT 1 + 2;
SELECT * FROM users;
SELECT id, name, email FROM users;
SELECT id AS user_id, name AS full_name FROM users;
SELECT DISTINCT name FROM users;
