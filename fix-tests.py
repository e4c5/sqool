with open("sqool-conformance/src/test/java/io/github/e4c5/sqool/conformance/mysql/MysqlConformanceTest.java", "r") as f:
    text = f.read()

target1 = 'new ResourceExpectation("mysql/unsupported/functions-and-exprs.sql", false, ""),'

text = text.replace(target1, '')

target2 = 'new ResourceCase("mysql/supported/administrative-statements.sql", true),'
rep2 = 'new ResourceCase("mysql/supported/administrative-statements.sql", true),\n                    new ResourceCase("mysql/supported/functions-and-exprs.sql", false),'

text = text.replace(target2, rep2)

with open("sqool-conformance/src/test/java/io/github/e4c5/sqool/conformance/mysql/MysqlConformanceTest.java", "w") as f:
    f.write(text)

