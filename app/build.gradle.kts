plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

val releaseSigningEnvironment = mapOf(
    "SCREENLOOM_UPLOAD_STORE_FILE" to providers.environmentVariable("SCREENLOOM_UPLOAD_STORE_FILE"),
    "SCREENLOOM_UPLOAD_STORE_PASSWORD" to providers.environmentVariable("SCREENLOOM_UPLOAD_STORE_PASSWORD"),
    "SCREENLOOM_UPLOAD_KEY_ALIAS" to providers.environmentVariable("SCREENLOOM_UPLOAD_KEY_ALIAS"),
    "SCREENLOOM_UPLOAD_KEY_PASSWORD" to providers.environmentVariable("SCREENLOOM_UPLOAD_KEY_PASSWORD"),
)
val missingReleaseSigningEnvironment = releaseSigningEnvironment.filterValues { !it.isPresent }.keys
val releaseSigningConfigured = missingReleaseSigningEnvironment.isEmpty()

android {
    namespace = "kr.donminzzi.screenloom"
    compileSdk = 36
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "kr.donminzzi.screenloom"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    signingConfigs {
        if (releaseSigningConfigured) {
            create("release") {
                storeFile = file(releaseSigningEnvironment.getValue("SCREENLOOM_UPLOAD_STORE_FILE").get())
                storePassword = releaseSigningEnvironment.getValue("SCREENLOOM_UPLOAD_STORE_PASSWORD").get()
                keyAlias = releaseSigningEnvironment.getValue("SCREENLOOM_UPLOAD_KEY_ALIAS").get()
                keyPassword = releaseSigningEnvironment.getValue("SCREENLOOM_UPLOAD_KEY_PASSWORD").get()
            }
        }
    }

    buildTypes {
        getByName("release") {
            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
}

val verifyReleaseSigningConfiguration = tasks.register("verifyReleaseSigningConfiguration") {
    group = "verification"
    description = "Prevents unsigned Screenloom release artifacts."

    doLast {
        check(releaseSigningConfigured) {
            "Release signing requires: ${missingReleaseSigningEnvironment.joinToString()}"
        }
    }
}

tasks.matching { it.name == "packageRelease" || it.name == "packageReleaseBundle" }.configureEach {
    dependsOn(verifyReleaseSigningConfiguration)
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.exifinterface)

    testImplementation(libs.junit4)

    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.compose.ui.test.junit4)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
}

val verifyDebugManifestPermissions = tasks.register("verifyDebugManifestPermissions") {
    group = "verification"
    description = "Verifies Screenloom's exact merged-manifest permission allowlist."
    dependsOn("processDebugMainManifest")

    doLast {
        // Must be the directory processDebugMainManifest itself writes (singular
        // "merged_manifest"). Reading the plural "merged_manifests" tree instead validates an
        // artifact no declared dependency refreshes, so the check silently passes on a stale
        // copy — verified by adding android.permission.INTERNET and watching it go green.
        val manifestCandidates = layout.buildDirectory
            .dir("intermediates/merged_manifest/debug")
            .get()
            .asFile
            .walkTopDown()
            .filter { candidate -> candidate.isFile && candidate.name == "AndroidManifest.xml" }
            .toList()
        check(manifestCandidates.size == 1) {
            "Expected one debug merged manifest but found ${manifestCandidates.size}: $manifestCandidates"
        }
        val manifestFile = manifestCandidates.single()
        val permissions = Regex("""<uses-permission\s+android:name="([^"]+)"""")
            .findAll(manifestFile.readText())
            .map { match -> match.groupValues[1] }
            .toSet()
        val allowedPermissions = setOf(
            "kr.donminzzi.screenloom.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION",
        )
        check(permissions == allowedPermissions) {
            "Unexpected permissions $permissions in ${manifestFile.absolutePath}; expected $allowedPermissions"
        }
    }
}

tasks.matching { it.name == "lintDebug" }.configureEach {
    dependsOn(verifyDebugManifestPermissions)
}
