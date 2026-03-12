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
}

group = "io.github.e4c5"
version = "0.1.0-SNAPSHOT"

val libsCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
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
}

configure(javaModules.map(::project)) {
    apply(plugin = "java-library")
    apply(plugin = "checkstyle")
    apply(plugin = "com.diffplug.spotless")

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion = JavaLanguageVersion.of(25)
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
        options.release = 25
        options.encoding = "UTF-8"
        options.compilerArgs.add("-Xlint:all")
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        val javaToolchainService = project.extensions.getByType<JavaToolchainService>()
        javaLauncher.set(
            javaToolchainService.launcherFor {
                languageVersion = JavaLanguageVersion.of(25)
            },
        )
        testLogging {
            events = setOf(TestLogEvent.FAILED, TestLogEvent.SKIPPED)
            exceptionFormat = TestExceptionFormat.FULL
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
            "-package",
            "io.github.e4c5.sqool.grammar.mysql.generated",
        )
    }

    tasks.withType<Checkstyle>().configureEach {
        exclude("**/generated-src/antlr/**")
        exclude("**/BootstrapSql*.java")
    }

    tasks.withType<Javadoc>().configureEach {
        exclude("**/generated-src/antlr/**")
    }
}

listOf(
    "sqool-grammar-postgresql",
    "sqool-grammar-oracle",
    "sqool-grammar-sqlite",
).forEach { moduleName ->
    project(":$moduleName") {
        dependencies {
            "api"(libsCatalog.findLibrary("antlr-runtime").get())
        }
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
    }
}

project(":sqool-bench") {
    apply(plugin = "java")
    apply(plugin = "me.champeau.jmh")
    apply(plugin = "com.diffplug.spotless")

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion = JavaLanguageVersion.of(25)
        }
    }

    dependencies {
        "implementation"(project(":sqool-core"))
    }

    tasks.withType<JavaCompile>().configureEach {
        options.release = 25
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
