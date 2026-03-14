-- SELECT without FROM
SELECT 1;
SELECT 1, 2, 'hello';
SELECT a+b FROM t1;

-- Multiple SET operators
SELECT * FROM a UNION ALL SELECT * FROM b UNION SELECT * FROM c;

-- Single-table UPDATE with ignore, where, order by, limit
UPDATE IGNORE t1 SET a = 1, b = 2 WHERE c = 3 ORDER BY d DESC LIMIT 10;
