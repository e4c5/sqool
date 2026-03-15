-- UPDATE statements (mapped as PostgresqlRawStatement)
UPDATE users SET name = 'Alice' WHERE id = 1;
UPDATE users SET name = 'Bob', email = 'bob@example.com' WHERE id = 2;
UPDATE orders SET total = 199.99 WHERE id = 10;
UPDATE users SET name = 'Carol' WHERE id = 3 RETURNING id, name;
