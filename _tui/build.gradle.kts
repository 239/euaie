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
    //WARNING: A restricted method in java.lang.System has been called
    //WARNING: java.lang.System::load has been called by org.jline.nativ.JLineNativeLoader in an unnamed module
    //WARNING: Use --enable-native-access=ALL-UNNAMED to avoid a warning for callers in this module
    //WARNING: Restricted methods will be blocked in a future release unless native access is enabled
    //https://jline.org/versions/4.0/docs/troubleshooting#jdk-24-restricted-method-warning
    applicationDefaultJvmArgs = listOf(
        "--enable-native-access=ALL-UNNAMED" //TODO ineffective?
    )
}

dependencies {
    implementation(project(":_api"))
//    implementation(libs.jline)
    implementation(libs.kotter.jvm)
    implementation(libs.picocli)
    implementation(libs.tinylog.api.kotlin)
    implementation(libs.tinylog.impl)
    kapt(libs.picocli.codegen)
    testImplementation(kotlin("test"))
    testImplementation(libs.kotterx.twemoji)
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

graalvmNative {
    val name = System.getProperty("os.name").lowercase()
    val arch = System.getProperty("os.arch").lowercase()
    val type = when {
        "linux" in name   -> "linux"
        "mac" in name     -> "macos"
        "windows" in name -> "windows"
        else              -> name
    }
    binaries {
        named("main") {
            imageName.set("${rootProject.name}-$type-$arch")
            useFatJar.set(false)
            if (type == "windows")
                buildArgs.add("-H:+AddAllCharsets") //UnsupportedCharsetException: Cp1252
        }
    }
    agent {
        enabled.set(true)
        metadataCopy {
            inputTaskNames.add("run") //system terminal does not work via Gradle
            outputDirectories.add("src/main/resources/META-INF/native-image/${rootProject.name}")
            mergeWithExisting.set(true)
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
