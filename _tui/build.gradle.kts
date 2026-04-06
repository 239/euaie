plugins {
    application
    alias(libs.plugins.kjvm)
    alias(libs.plugins.kapt)
    alias(libs.plugins.shadow)
    alias(libs.plugins.ggp)
}

kotlin { jvmToolchain(25) }

application {
    mainClass = "euaie.CLIKt"
}

dependencies {
    implementation(project(":_api"))
    implementation(libs.jline)
    implementation(libs.kotter.jvm)
    implementation(libs.picocli)
    implementation(libs.tinylog.api.kotlin)
    implementation(libs.tinylog.impl)
    kapt(libs.picocli.codegen)
    testImplementation(kotlin("test"))
    testImplementation(libs.truthish)
}

kapt {
    arguments {
        arg("project", "${project.group}")
        arg("disable.proxy.config")
    }
}

gitProperties {
    dateFormat = "yy.DD"
    keys = listOf("git.branch", "git.commit.id.abbrev", "git.commit.time")
}

tasks.shadowJar {
    archiveBaseName = rootProject.name
    archiveClassifier = ""
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    mergeServiceFiles()
    filesMatching("META-INF/services/**") {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
}

tasks.test { outputs.upToDateWhen { false } }
