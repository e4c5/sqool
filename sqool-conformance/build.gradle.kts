dependencies {
    "implementation"(project(":sqool-core"))
    "implementation"(project(":sqool-ast"))
    "testImplementation"(project(":sqool-dialect-mysql"))
}

tasks.named<org.gradle.testing.jacoco.tasks.JacocoReport>("jacocoTestReport") {
    additionalSourceDirs.from(project(":sqool-dialect-mysql").sourceSets.main.get().allSource.srcDirs)
    sourceDirectories.from(project(":sqool-dialect-mysql").sourceSets.main.get().allSource.srcDirs)
    classDirectories.from(project(":sqool-dialect-mysql").sourceSets.main.get().output)
}
