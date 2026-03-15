-- SELECT with ORDER BY, LIMIT
SELECT id, name FROM users ORDER BY name ASC;
SELECT id, name FROM users ORDER BY name DESC;
SELECT id, name FROM users ORDER BY name ASC, id DESC;
SELECT id FROM users LIMIT 10;
SELECT id FROM users ORDER BY id ASC LIMIT 20;
