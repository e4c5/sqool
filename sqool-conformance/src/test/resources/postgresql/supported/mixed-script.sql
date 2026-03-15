-- Mixed script: DDL + DML
CREATE TABLE products (
    id SERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    price NUMERIC(10, 2) NOT NULL
);
INSERT INTO products (name, price) VALUES ('Widget', 9.99);
INSERT INTO products (name, price) VALUES ('Gadget', 29.99);
SELECT id, name, price FROM products WHERE price < 20.00 ORDER BY price ASC;
UPDATE products SET price = 8.99 WHERE name = 'Widget';
SELECT * FROM products;
DELETE FROM products WHERE id = 1;
DROP TABLE products;
