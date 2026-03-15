import com.diffplug.gradle.spotless.SpotlessExtension
import me.champeau.jmh.JmhParameters
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.antlr.AntlrTask
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.plugins.quality.Checkstyle
import org.gradle.api.plugins.quality.CheckstyleExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named

plugins {
    base
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.jmh) apply false
    jacoco
}

group = "io.github.e4c5"
version = "0.1.0-SNAPSHOT"

// Modules published to Maven (see docs/release-readiness.md).
// Dialect modules depend on their grammar modules, so those grammars must be published too.
val publishableModules = listOf(
    "sqool-core",
    "sqool-ast",
    "sqool-grammar-mysql",
    "sqool-grammar-postgresql",
    "sqool-grammar-oracle",
    "sqool-grammar-sqlite",
    "sqool-dialect-mysql",
    "sqool-dialect-postgresql",
    "sqool-dialect-oracle",
    "sqool-dialect-sqlite",
)

val libsCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
val targetJavaVersion = 25
val targetJavaLanguageVersion = JavaLanguageVersion.of(targetJavaVersion)
val javaModules = listOf(
    "sqool-core",
    "sqool-ast",
    "sqool-grammar-mysql",
    "sqool-grammar-postgresql",
    "sqool-grammar-oracle",
    "sqool-grammar-sqlite",
    "sqool-dialect-mysql",
    "sqool-dialect-postgresql",
    "sqool-dialect-oracle",
    "sqool-dialect-sqlite",
    "sqool-conformance",
)

allprojects {
    group = rootProject.group
    version = rootProject.version
    dependencyLocking {
        lockAllConfigurations()
    }
}

configure(javaModules.map(::project)) {
    apply(plugin = "java-library")
    apply(plugin = "checkstyle")
    apply(plugin = "com.diffplug.spotless")
    apply(plugin = "jacoco")

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion = targetJavaLanguageVersion
        }
        withJavadocJar()
        withSourcesJar()
    }

    extensions.configure<CheckstyleExtension> {
        toolVersion = libsCatalog.findVersion("checkstyle").get().requiredVersion
        configFile = rootProject.file("config/checkstyle/checkstyle.xml")
        isShowViolations = true
    }

    dependencies {
        "testImplementation"(platform(libsCatalog.findLibrary("junit-bom").get()))
        "testImplementation"(libsCatalog.findLibrary("junit-jupiter").get())
        "testRuntimeOnly"(libsCatalog.findLibrary("junit-platform-launcher").get())
    }

    tasks.withType<JavaCompile>().configureEach {
        options.release = targetJavaVersion
        options.encoding = "UTF-8"
        options.compilerArgs.add("-Xlint:all")
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        val javaToolchainService = project.extensions.getByType<JavaToolchainService>()
        javaLauncher.set(
            javaToolchainService.launcherFor {
                languageVersion = targetJavaLanguageVersion
            },
        )
        testLogging {
            events = setOf(TestLogEvent.FAILED, TestLogEvent.SKIPPED)
            exceptionFormat = TestExceptionFormat.FULL
        }
        addTestListener(object : TestListener {
            override fun beforeSuite(suite: TestDescriptor) {}
            override fun beforeTest(testDescriptor: TestDescriptor) {}
            override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {}
            override fun afterSuite(suite: TestDescriptor, result: TestResult) {
                if (suite.parent == null) { // root suite
                    println("\nResults: ${result.resultType} (${result.testCount} tests, ${result.successfulTestCount} successes, ${result.failedTestCount} failures, ${result.skippedTestCount} skipped)")
                }
            }
        })
        finalizedBy(tasks.named("jacocoTestReport"))
    }

    tasks.named<org.gradle.testing.jacoco.tasks.JacocoReport>("jacocoTestReport") {
        dependsOn(tasks.named("test"))
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }

    tasks.named("check") {
        dependsOn("spotlessCheck")
    }

    extensions.configure<SpotlessExtension> {
        java {
            target("src/*/java/**/*.java")
            googleJavaFormat()
            trimTrailingWhitespace()
            endWithNewline()
        }
        format("misc") {
            target("*.gradle.kts", "*.md", ".gitignore", "gradle/*.toml")
            trimTrailingWhitespace()
            endWithNewline()
        }
    }
}

project(":sqool-core") {
    dependencies {
        "api"(project(":sqool-ast"))
        "api"(libsCatalog.findLibrary("antlr-runtime").get())
    }
}

project(":sqool-grammar-mysql") {
    apply(plugin = "antlr")

    dependencies {
        "antlr"(libsCatalog.findLibrary("antlr-tool").get())
        "api"(libsCatalog.findLibrary("antlr-runtime").get())
    }

    tasks.withType<AntlrTask>().configureEach {
        arguments = arguments + listOf(
            "-visitor",
            "-long-messages",
        )
    }

    tasks.named<Checkstyle>("checkstyleMain") {
        source = files().asFileTree
    }

    tasks.named<Javadoc>("javadoc") {
        source = files().asFileTree
    }
}

project(":sqool-grammar-sqlite") {
    apply(plugin = "antlr")

    dependencies {
        "antlr"(libsCatalog.findLibrary("antlr-tool").get())
        "api"(libsCatalog.findLibrary("antlr-runtime").get())
    }

    tasks.withType<AntlrTask>().configureEach {
        arguments = arguments + listOf(
            "-visitor",
            "-long-messages",
        )
    }

    tasks.named<Checkstyle>("checkstyleMain") {
        source = files().asFileTree
    }

    tasks.named<Javadoc>("javadoc") {
        source = files().asFileTree
    }
}

project(":sqool-grammar-postgresql") {
    apply(plugin = "antlr")

    dependencies {
        "antlr"(libsCatalog.findLibrary("antlr-tool").get())
        "api"(libsCatalog.findLibrary("antlr-runtime").get())
    }

    tasks.withType<AntlrTask>().configureEach {
        arguments = arguments + listOf(
            "-visitor",
            "-long-messages",
        )
    }

    tasks.named<Checkstyle>("checkstyleMain") {
        source = files().asFileTree
    }

    tasks.named<Javadoc>("javadoc") {
        source = files().asFileTree
    }
}

project(":sqool-grammar-oracle") {
    apply(plugin = "antlr")

    dependencies {
        "antlr"(libsCatalog.findLibrary("antlr-tool").get())
        "api"(libsCatalog.findLibrary("antlr-runtime").get())
    }

    tasks.withType<AntlrTask>().configureEach {
        arguments = arguments + listOf(
            "-visitor",
            "-long-messages",
        )
    }

    tasks.named<Checkstyle>("checkstyleMain") {
        source = files().asFileTree
    }

    tasks.named<Javadoc>("javadoc") {
        source = files().asFileTree
    }
}

listOf(
    "sqool-dialect-mysql",
    "sqool-dialect-postgresql",
    "sqool-dialect-oracle",
    "sqool-dialect-sqlite",
).forEach { moduleName ->
    project(":$moduleName") {
        dependencies {
            "implementation"(project(":sqool-core"))
            "implementation"(project(":sqool-ast"))
        }
    }
}

project(":sqool-dialect-mysql") {
    dependencies {
        "implementation"(project(":sqool-grammar-mysql"))
    }
}

project(":sqool-dialect-postgresql") {
    dependencies {
        "implementation"(project(":sqool-grammar-postgresql"))
    }
}

project(":sqool-dialect-oracle") {
    dependencies {
        "implementation"(project(":sqool-grammar-oracle"))
    }
}

project(":sqool-dialect-sqlite") {
    dependencies {
        "implementation"(project(":sqool-grammar-sqlite"))
    }
}

project(":sqool-conformance") {
    dependencies {
        "implementation"(project(":sqool-core"))
        "implementation"(project(":sqool-ast"))
        "testImplementation"(project(":sqool-dialect-mysql"))
        "testImplementation"(project(":sqool-dialect-postgresql"))
        "testImplementation"(project(":sqool-dialect-sqlite"))
        "testImplementation"(project(":sqool-dialect-oracle"))
    }

    tasks.named<org.gradle.testing.jacoco.tasks.JacocoReport>("jacocoTestReport") {
        dependsOn(tasks.named("test"))
        val mysqlDialect = project(":sqool-dialect-mysql")
        val mysqlJavaExt = mysqlDialect.extensions.getByType<JavaPluginExtension>()
        sourceDirectories.from(mysqlJavaExt.sourceSets.getByName("main").allSource.srcDirs)
        classDirectories.from(mysqlJavaExt.sourceSets.getByName("main").output)
        val postgresqlDialect = project(":sqool-dialect-postgresql")
        val postgresqlJavaExt = postgresqlDialect.extensions.getByType<JavaPluginExtension>()
        sourceDirectories.from(postgresqlJavaExt.sourceSets.getByName("main").allSource.srcDirs)
        classDirectories.from(postgresqlJavaExt.sourceSets.getByName("main").output)
        val sqliteDialect = project(":sqool-dialect-sqlite")
        val sqliteJavaExt = sqliteDialect.extensions.getByType<JavaPluginExtension>()
        sourceDirectories.from(sqliteJavaExt.sourceSets.getByName("main").allSource.srcDirs)
        classDirectories.from(sqliteJavaExt.sourceSets.getByName("main").output)
        val oracleDialect = project(":sqool-dialect-oracle")
        val oracleJavaExt = oracleDialect.extensions.getByType<JavaPluginExtension>()
        sourceDirectories.from(oracleJavaExt.sourceSets.getByName("main").allSource.srcDirs)
        classDirectories.from(oracleJavaExt.sourceSets.getByName("main").output)
    }
}

project(":sqool-bench") {
    apply(plugin = "java")
    apply(plugin = "me.champeau.jmh")
    apply(plugin = "com.diffplug.spotless")

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion = targetJavaLanguageVersion
        }
    }

    dependencies {
        "implementation"(project(":sqool-core"))
        "implementation"(project(":sqool-dialect-mysql"))
        "implementation"(project(":sqool-dialect-postgresql"))
        "implementation"(project(":sqool-dialect-sqlite"))
        "implementation"(project(":sqool-dialect-oracle"))
        "implementation"(libsCatalog.findLibrary("jsqlparser").get())
    }

    tasks.withType<JavaCompile>().configureEach {
        options.release = targetJavaVersion
        options.encoding = "UTF-8"
    }

    tasks.named("check") {
        dependsOn("spotlessCheck")
    }

    extensions.configure<SpotlessExtension> {
        java {
            target("src/**/*.java")
            googleJavaFormat()
            trimTrailingWhitespace()
            endWithNewline()
        }
        format("misc") {
            target("*.gradle.kts")
            trimTrailingWhitespace()
            endWithNewline()
        }
    }

    extensions.configure<JmhParameters>("jmh") {
        jmhVersion.set(libsCatalog.findVersion("jmh").get().requiredVersion)
        fork.set(1)
        warmupForks.set(0)
        warmupIterations.set(1)
        iterations.set(1)
        duplicateClassesStrategy.set(DuplicatesStrategy.EXCLUDE)
    }
}

configure(publishableModules.map(::project)) {
    apply(plugin = "maven-publish")
    extensions.configure<org.gradle.api.publish.PublishingExtension> {
        publications {
            create("maven", org.gradle.api.publish.maven.MavenPublication::class.java) {
                from(components["java"])
                groupId = rootProject.group.toString()
                artifactId = project.name
                version = rootProject.version.toString()
                pom {
                    name.set(project.name)
                    description.set("ANTLR-based SQL parser for Java — ${project.name}")
                    url.set("https://github.com/e4c5/sqool")
                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    scm {
                        url.set("https://github.com/e4c5/sqool")
                        connection.set("scm:git:https://github.com/e4c5/sqool.git")
                    }
                    developers {
                        developer {
                            id.set("e4c5")
                            name.set("Raditha Dissanayake")
                            email.set("raditha.d@gmail.com")
                            url.set("https://github.com/e4c5")
                        }
                    }
                }
            }
        }
    }
}

tasks.register("verifyBootstrap") {
    group = "verification"
    description = "Runs the Milestone 0 bootstrap verification tasks."
    dependsOn(
        ":sqool-grammar-mysql:generateGrammarSource",
        ":sqool-core:test",
        ":sqool-grammar-mysql:test",
        ":sqool-bench:jmh",
    )
}
