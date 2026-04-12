import java.nio.file.*
import java.nio.file.attribute.*

plugins {
//    embeddedKotlin("jvm") apply false
//    kotlin("jvm") version "2.3.20" apply false
    alias(libs.plugins.kjvm) apply false
    alias(libs.plugins.kapt) apply false
    alias(libs.plugins.versions)
}

allprojects {
    repositories {
        mavenCentral()
        maven("https://central.sonatype.com/repository/maven-snapshots/") { //TODO !
            mavenContent {
                includeGroup("com.varabyte.kotter")
                snapshotsOnly()
            }
        }
    }

    configurations.configureEach {
        resolutionStrategy.cacheChangingModulesFor(3, "hours")
    }

    //reproducible
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
