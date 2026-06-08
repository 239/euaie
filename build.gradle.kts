plugins {
    alias(libs.plugins.kjvm) apply false
    alias(libs.plugins.kapt) apply false
    alias(libs.plugins.versions)
}

allprojects {
    repositories {
        mavenCentral()
        maven("https://central.sonatype.com/repository/maven-snapshots/") {
            mavenContent {
                snapshotsOnly()
                includeGroup("com.varabyte.kotter")
                includeGroup("com.varabyte.kotterx")
            }
        }
    }

    configurations.configureEach {
        resolutionStrategy.cacheChangingModulesFor(1, "hours")
    }

    // reproducible jar
    tasks.withType<AbstractArchiveTask>().configureEach {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
        eachFile { permissions { unix("644") } }
        dirPermissions { unix("755") }
    }
}
