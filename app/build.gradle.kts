plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.greenicephoenix.traceledger"
    compileSdk { version = release(36) }

    defaultConfig {
        applicationId = "com.greenicephoenix.traceledger"
        minSdk = 26
        targetSdk = 36
        versionCode = 6
        versionName = "1.3.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("boolean", "IS_PLAY_STORE_BUILD", "false")

        // Required for Apache POI on Android — prevents DEX method count issues
        multiDexEnabled = true
    }

    signingConfigs {
        create("release") {
            storeFile = file("../traceledger-release.jks")
            storePassword = System.getenv("TRACELEDGER_STORE_PASSWORD")
            keyAlias = "traceledger"
            keyPassword = System.getenv("TRACELEDGER_KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions { jvmTarget = "11" }
    buildFeatures { compose = true; buildConfig = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
        arg("room.incremental", "true")
        arg("room.expandProjection", "true")
    }

    // Apache POI includes duplicate files — tell the build system which to keep.
    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/*.RSA",
                "META-INF/*.SF",
                "META-INF/*.DSA",
                "META-INF/versions/9/module-info.class"
            )
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation(libs.compose.ui)
    implementation(libs.compose.preview)
    implementation(libs.material3)
    implementation(libs.navigation.compose)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.material.icons)
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.pdfbox)

    // ── Apache POI — XLS + encrypted XLSX support ─────────────────────────────
    // poi       = core module, handles .xls (HSSF) + encryption/decryption layer
    // poi-ooxml = handles .xlsx (XSSF) + reading decrypted OOXML content
    //
    // Exclusions explained:
    //   xmlbeans          — OOXML schema validator, not needed for reading, huge
    //   logging           — POI uses log4j which conflicts with Android logging
    //   curvesapi         — unused curve math library pulled by poi-ooxml
    //   poi-ooxml-full    — full schema model; poi-ooxml-lite is enough for reading
    //
    // If build fails with "duplicate class" errors, add more entries to the
    // packaging.resources.excludes block above.
    implementation(libs.poi.core) {
        exclude(group = "org.apache.logging.log4j")
        exclude(group = "commons-logging")
    }
    implementation(libs.poi.ooxml) {
        exclude(group = "org.apache.xmlbeans")
        exclude(group = "org.apache.logging.log4j")
        exclude(group = "com.github.virtuald")
        exclude(group = "commons-logging")
        exclude(group = "org.apache.poi", module = "poi-ooxml-full")
    }
    implementation("org.apache.poi:poi:5.2.5")
    implementation("org.apache.poi:poi-ooxml:5.2.5")
    implementation("org.apache.logging.log4j:log4j-api:2.23.1")
    implementation("org.apache.xmlbeans:xmlbeans:5.2.1")
}