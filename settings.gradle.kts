pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

rootProject.name = "sqool"

include(
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
    "sqool-bench",
)
