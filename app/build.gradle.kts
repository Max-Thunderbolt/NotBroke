plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.notbroke"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.notbroke"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    // Configure lint options
    lint {
        abortOnError = false
        checkReleaseBuilds = false
        baseline = file("lint-baseline.xml")
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // Disable resource optimization
    androidResources {
        noCompress += listOf("json")
        ignoreAssetsPattern = "!.svn:!.git:!.ds_store:!*.scc:.*:!CVS:!thumbs.db:!picasa.ini:!*~"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    
    // Import the Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:32.7.2"))
    
    // Firebase Auth
    implementation("com.google.firebase:firebase-auth-ktx")
    
    // Firebase Firestore
    implementation("com.google.firebase:firebase-firestore-ktx")
    
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}

// Configure tasks to handle incremental builds properly
tasks.configureEach {
    when {
        name.contains("merge", ignoreCase = true) ||
        name.contains("process", ignoreCase = true) ||
        name.contains("package", ignoreCase = true) -> {
            outputs.upToDateWhen { false }
        }
    }
}