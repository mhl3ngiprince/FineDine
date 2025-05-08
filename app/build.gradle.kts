plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.finedine.rms"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.finedine.rms"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf(
                    "room.schemaLocation" to "$projectDir/schemas",
                    "room.incremental" to "true",
                    "room.expandProjection" to "true"
                )
            }
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/LICENSE*"
            excludes += "/META-INF/NOTICE*"
        }
    }

    applicationVariants.all {
        val variant = this
        val task = tasks.register("print${variant.name.capitalize()}CertificateFingerprint") {
            doLast {
                val keystorePath = if (variant.name.contains("release")) {
                    // Path to your release keystore if applicable
                    "${project.projectDir}/your-release-key.jks"
                } else {
                    // Default debug keystore path
                    "${System.getProperty("user.home")}/.android/debug.keystore"
                }

                val keystorePassword = if (variant.name.contains("release")) {
                    // Your release keystore password
                    "your-release-password"
                } else {
                    // Default debug keystore password
                    "android"
                }

                val keystoreAlias = if (variant.name.contains("release")) {
                    // Your release key alias
                    "your-key-alias"
                } else {
                    // Default debug key alias
                    "androiddebugkey"
                }

                exec {
                    workingDir = project.rootDir
                    if (org.gradle.internal.os.OperatingSystem.current().isWindows) {
                        commandLine(
                            "cmd",
                            "/c",
                            "keytool -list -v -keystore \"$keystorePath\" -alias $keystoreAlias -storepass $keystorePassword -keypass $keystorePassword"
                        )
                    } else {
                        commandLine(
                            "bash",
                            "-c",
                            "keytool -list -v -keystore $keystorePath -alias $keystoreAlias -storepass $keystorePassword -keypass $keystorePassword"
                        )
                    }
                }
            }
            group = "Verification"
            description = "Prints the ${variant.name} certificate fingerprints for Firebase setup"
        }
    }
}

tasks.register("printCertificateFingerprint") {
    dependsOn("printDebugCertificateFingerprint")
    group = "Verification"
    description = "Prints the debug certificate fingerprints for Firebase setup"
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.biometric:biometric:1.2.0-alpha05")

    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.activity:activity:1.8.0")
    implementation("androidx.fragment:fragment:1.6.2")

    // ViewModel and LiveData
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.6.2")
    implementation("androidx.lifecycle:lifecycle-livedata:2.6.2")

    // Room Database
    implementation("androidx.room:room-runtime:2.6.0")
    annotationProcessor("androidx.room:room-compiler:2.6.0")

    // Retrofit for API calls
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Navigation components
    implementation("androidx.navigation:navigation-fragment:2.7.7")
    implementation("androidx.navigation:navigation-ui:2.7.7")
    implementation("androidx.drawerlayout:drawerlayout:1.2.0")

    // Firebase dependencies using BoM
    implementation(platform("com.google.firebase:firebase-bom:33.12.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-messaging")

    // Firebase Database (ensure these are explicitly included)
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-database-ktx")

    // Other Firebase modules
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.inappmessaging)
    implementation(libs.material)

    // Glide for image loading
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // JavaMail for email functionality
    implementation("com.sun.mail:android-mail:1.6.6")
    implementation("com.sun.mail:android-activation:1.6.6")

    // CSV handling
    implementation("com.opencsv:opencsv:5.6")

    // JSON parsing
    implementation("com.google.code.gson:gson:2.10.1")

    // Security
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // UI components
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // Charts
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Testing
    testImplementation(libs.junit)
}