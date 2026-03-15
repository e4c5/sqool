-- INSERT with RETURNING clause (PostgreSQL-specific extension)
INSERT INTO users (name, email) VALUES ('Alice', 'alice@example.com') RETURNING id;
INSERT INTO users (name) VALUES ('Bob') RETURNING id, name;
INSERT INTO orders (user_id, total) VALUES (1, 50.00) RETURNING *;
