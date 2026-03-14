-- SELECT with WHERE predicates
SELECT id FROM users WHERE id = 1;
SELECT id FROM users WHERE id > 10;
SELECT id FROM users WHERE id >= 10;
SELECT id FROM users WHERE id < 100;
SELECT id FROM users WHERE id <= 100;
SELECT id FROM users WHERE id <> 0;
SELECT id FROM users WHERE id != 0;
SELECT id FROM users WHERE id = 1 AND name = 'Alice';
SELECT id FROM users WHERE id = 1 OR id = 2;
SELECT id FROM users WHERE NOT id = 0;
