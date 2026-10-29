import java.io.File
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.util.Base64
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "ru.mlbb.blocker"
    compileSdk = 35

    defaultConfig {
        applicationId = "ru.mlbb.blocker"
        minSdk = 24
        targetSdk = 35
        versionCode = 25
        versionName = "0.17.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val metaApiBase = (project.findProperty("metaApiBase") as String?)
            ?: System.getenv("META_API_BASE")
            ?: "https://your-domain.com"
        buildConfigField("String", "META_API_BASE", "\"$metaApiBase\"")
    }

    signingConfigs {
        val propsFile = rootProject.file("keystore.properties")
        val props = Properties().apply {
            if (propsFile.exists()) propsFile.inputStream().use { load(it) }
        }
        val storePath = props.getProperty("storeFile") ?: System.getenv("RELEASE_KEYSTORE_PATH")
        if (!storePath.isNullOrBlank()) {
            create("release") {
                storeFile = rootProject.file(storePath)
                storePassword = props.getProperty("storePassword") ?: System.getenv("RELEASE_STORE_PASSWORD")
                keyAlias = props.getProperty("keyAlias") ?: System.getenv("RELEASE_KEY_ALIAS")
                keyPassword = props.getProperty("keyPassword") ?: System.getenv("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            signingConfigs.findByName("release")?.let { signingConfig = it }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
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
        compose = true
        buildConfig = true
    }

    androidResources {
        generateLocaleConfig = false
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }

    splits {
        abi {
            isEnable = true
            isUniversalApk = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86_64")
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    lint {
        abortOnError = false
    }

}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(files("libs/libbox.aar"))

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.work:work-runtime-ktx:2.10.0")

    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    implementation("androidx.room:room-runtime:2.7.2")
    implementation("androidx.room:room-ktx:2.7.2")
    ksp("androidx.room:room-compiler:2.7.2")

    implementation("com.maxmind.db:maxmind-db:3.1.1")
    implementation("io.coil-kt:coil-compose:2.7.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")

    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.12.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}


// Подпись rules.json тем же ключом, что и APK. Результат публикуется в GitHub-релизе
// как rules.json и rules.json.sig; приложение проверяет подпись сертификатом APK.
val signRules by tasks.registering {
    group = "distribution"
    description = "Signs rules.json with the release key for publishing as release assets."

    val sourceFile = layout.projectDirectory.file("src/main/assets/rules/rules.json")
    val outputDir = layout.buildDirectory.dir("outputs/rules")
    inputs.file(sourceFile).withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.dir(outputDir)

    doLast {
        val propsFile = rootProject.file("keystore.properties")
        val props = Properties().apply {
            if (propsFile.exists()) propsFile.inputStream().use { load(it) }
        }
        val storePath = props.getProperty("storeFile") ?: System.getenv("RELEASE_KEYSTORE_PATH")
        val storePassword = props.getProperty("storePassword") ?: System.getenv("RELEASE_STORE_PASSWORD")
        val keyAlias = props.getProperty("keyAlias") ?: System.getenv("RELEASE_KEY_ALIAS")
        val keyPassword = props.getProperty("keyPassword") ?: System.getenv("RELEASE_KEY_PASSWORD")

        if (storePath.isNullOrBlank() || storePassword.isNullOrBlank() ||
            keyAlias.isNullOrBlank() || keyPassword.isNullOrBlank()
        ) {
            throw GradleException("Release keystore is not configured: set keystore.properties or RELEASE_* env vars")
        }

        val storeFile = rootProject.file(storePath)
        if (!storeFile.exists()) throw GradleException("Keystore not found: ${storeFile.absolutePath}")

        val keyStore = KeyStore.getInstance(storeFile, storePassword.toCharArray())
        val privateKey = keyStore.getKey(keyAlias, keyPassword.toCharArray()) as? PrivateKey
            ?: throw GradleException("Alias '$keyAlias' is not a private key")
        val algorithm = when (privateKey.algorithm.uppercase()) {
            "EC", "ECDSA" -> "SHA256withECDSA"
            else -> "SHA256withRSA"
        }

        val content = sourceFile.asFile.readBytes()
        val signature = Signature.getInstance(algorithm).run {
            initSign(privateKey)
            update(content)
            sign()
        }
        val encoded = Base64.getEncoder().encodeToString(signature)

        val target = outputDir.get().asFile
        target.mkdirs()
        File(target, "rules.json").writeBytes(content)
        File(target, "rules.json.sig").writeText(encoded + "\n")
        logger.lifecycle("Signed rules.json with $algorithm, signature ${encoded.length} chars")
    }
}
