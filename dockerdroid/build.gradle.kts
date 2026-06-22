// Top-level build file. Plugin versions are declared in gradle/libs.versions.toml
// and applied (without `apply`) so the classpath resolves once for all modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}
