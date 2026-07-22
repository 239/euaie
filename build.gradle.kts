plugins {
    alias(libs.plugins.kjvm) apply false
    alias(libs.plugins.kapt) apply false
    alias(libs.plugins.versions)
}

allprojects {
    repositories {
        mavenCentral()
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
