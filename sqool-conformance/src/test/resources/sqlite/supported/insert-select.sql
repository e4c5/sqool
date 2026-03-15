-- INSERT and SELECT for SQLite conformance
INSERT INTO users (id, name, email) VALUES (1, 'alice', 'alice@example.com');
INSERT INTO users VALUES (2, 'bob', 'bob@example.com');

SELECT * FROM users WHERE id = 1;
SELECT name FROM users ORDER BY name LIMIT 10;
