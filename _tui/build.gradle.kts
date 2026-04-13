plugins {
    application
    alias(libs.plugins.ggp)
    alias(libs.plugins.kapt)
    alias(libs.plugins.kjvm)
    alias(libs.plugins.native)
    alias(libs.plugins.shadow)
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

val platform = {
    val name = System.getProperty("os.name").lowercase()
    val arch = System.getProperty("os.arch").lowercase()
    val system = when {
        "linux" in name   -> "linux"
        "mac" in name     -> "macos"
        "windows" in name -> "windows"
        else              -> name
    }
    "$system-$arch"
}

graalvmNative {
    agent {
        enabled.set(false) //TODO add to run?
    }
    binaries {
        named("main") {
            imageName.set("${rootProject.name}-$platform")
            useFatJar.set(false)
        }
    }
}

tasks.nativeCompile {
    dependsOn(tasks.shadowJar)
    classpathJar.set(tasks.shadowJar.flatMap { it.archiveFile })
}

tasks.shadowJar {
    archiveBaseName = rootProject.name
    archiveClassifier = ""
    destinationDirectory = layout.buildDirectory.dir("jar")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    mergeServiceFiles()
    filesMatching("META-INF/services/**") {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
}

tasks.test { outputs.upToDateWhen { false } }
