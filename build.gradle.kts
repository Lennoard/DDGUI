plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false
}

tasks.register<Delete>("clean") {
    description = "Cleans the build directory"
    delete(rootProject.layout.buildDirectory)
}
