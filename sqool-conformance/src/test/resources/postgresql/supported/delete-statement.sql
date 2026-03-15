-- DELETE statements (mapped as PostgresqlRawStatement)
DELETE FROM users WHERE id = 1;
DELETE FROM users WHERE id > 100;
DELETE FROM orders WHERE total < 0;
DELETE FROM users WHERE id = 5 RETURNING id;
