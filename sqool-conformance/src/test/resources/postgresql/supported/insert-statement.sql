-- INSERT statements (mapped as PostgresqlRawStatement)
INSERT INTO users (name, email) VALUES ('Alice', 'alice@example.com');
INSERT INTO users (name, email) VALUES ('Bob', 'bob@example.com'), ('Carol', 'carol@example.com');
INSERT INTO orders (user_id, total) VALUES (1, 99.99);
