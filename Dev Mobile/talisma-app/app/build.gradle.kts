import java.util.Properties
import java.net.URI

plugins {
    id("com.android.application")
}

// Catálogo e Lia usam a mesma origem HTTPS da AWS.
val propriedadesLocais = Properties().apply {
    val arquivo = rootProject.file("local.properties")
    if (arquivo.exists()) arquivo.inputStream().use { load(it) }
}
val apiUrl = (providers.gradleProperty("apiUrl").orNull
    ?: providers.environmentVariable("API_URL").orNull
    ?: propriedadesLocais.getProperty("api.url", "")).trim().trimEnd('/')
val origem = runCatching { URI(apiUrl) }.getOrNull()
require(origem != null && origem.scheme == "https" && origem.host != null &&
    origem.rawPath == "" && origem.rawQuery == null && origem.rawFragment == null &&
    origem.rawUserInfo == null) {
    "Configure API_URL, -PapiUrl ou api.url com a origem HTTPS da AWS, sem /api/v1."
}

// Keystore de demonstração commitado (D42): uso só acadêmico, nunca na Play Store
val pastaKeystore = rootProject.file("keystore")
val propriedadesKeystore = Properties().apply {
    pastaKeystore.resolve("keystore.properties").inputStream().use { load(it) }
}

android {
    namespace = "br.com.talisma.imoveis"
    compileSdk = 36

    defaultConfig {
        applicationId = "br.com.talisma.imoveis"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "API_URL", "\"$apiUrl\"")
    }

    signingConfigs {
        create("demonstracao") {
            storeFile = pastaKeystore.resolve(propriedadesKeystore.getProperty("storeFile"))
            storePassword = propriedadesKeystore.getProperty("storePassword")
            keyAlias = propriedadesKeystore.getProperty("keyAlias")
            keyPassword = propriedadesKeystore.getProperty("keyPassword")
        }
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("demonstracao")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity:1.9.3")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("org.osmdroid:osmdroid-android:6.1.20")
}
