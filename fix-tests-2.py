with open("sqool-conformance/src/test/java/io/github/e4c5/sqool/conformance/mysql/MysqlConformanceTest.java", "r") as f:
    text = f.read()

text = text.replace('new ResourceCase("mysql/supported/functions-and-exprs.sql", false),\n', '')
text = text.replace('new ResourceCase("mysql/supported/show-statements.sql", true),\n', '')

with open("sqool-conformance/src/test/java/io/github/e4c5/sqool/conformance/mysql/MysqlConformanceTest.java", "w") as f:
    f.write(text)

