//import java.nio.file.*
//import java.nio.file.attribute.*

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

    //reproducible //TODO enable again / Unix only?
//    tasks.withType<AbstractArchiveTask>().configureEach {
//        isPreserveFileTimestamps = false
//        isReproducibleFileOrder = true
//        eachFile {
//            permissions {
//                val exe = Files.getPosixFilePermissions(file.toPath()).contains(PosixFilePermission.OWNER_EXECUTE)
//                unix(if (exe) "755" else "644")
//            }
//        }
//        dirPermissions { unix("755") }
//    }
}
