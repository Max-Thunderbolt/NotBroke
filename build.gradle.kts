// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.9.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
<<<<<<< HEAD
    alias(libs.plugins.google.gms.google.services) apply false
=======
>>>>>>> dfb36664f5bd8ecf892bd63e6b9a33f2eefe4dac
}

tasks.withType<Wrapper> {
    gradleVersion = "8.2"
    distributionType = Wrapper.DistributionType.BIN
}
