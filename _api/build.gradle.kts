plugins {
    alias(libs.plugins.kjvm)
}

kotlin { jvmToolchain(25) }

dependencies {
    implementation(libs.tinylog.api.kotlin)
    implementation(libs.tinylog.impl)
}
