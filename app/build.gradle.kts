import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
}

// local.properties(VCS 미커밋)에서 Maps API 키를 읽어옵니다. 없으면 빈 문자열로 폴백합니다.
val mapsApiKey: String =
    Properties().apply {
        val localProps = rootProject.file("local.properties")
        if (localProps.exists()) {
            FileInputStream(localProps).use { load(it) }
        }
    }.getProperty("MAPS_API_KEY", "")

// keystore.properties(VCS 미커밋)에서 release 업로드 서명 설정을 읽어옵니다.
// 파일이 없으면(예: CI, 디버그 전용 환경) 서명 설정을 붙이지 않습니다.
val keystoreProps =
    Properties().apply {
        val f = rootProject.file("keystore.properties")
        if (f.exists()) {
            FileInputStream(f).use { load(it) }
        }
    }
val hasReleaseKeystore = keystoreProps.getProperty("storeFile") != null

android {
    namespace = "kr.yooreka.speedo"
    compileSdk = 36

    defaultConfig {
        applicationId = "kr.yooreka.speedo"
        minSdk = 27
        targetSdk = 35
        versionCode = 4
        versionName = "0.2.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // AndroidManifest의 ${MAPS_API_KEY} 자리에 주입됩니다.
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey

        // 광고 노출 여부. 알파 테스트 기간에는 광고를 노출하지 않는다(false).
        // 정식 출시 시 true 로 변경(또는 release 빌드타입에서 override).
        buildConfigField("boolean", "ADS_ENABLED", "false")
    }

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (hasReleaseKeystore) {
                signingConfig = signingConfigs.getByName("release")
            }
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
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.play.services.location)
    implementation(libs.androidx.navigation.compose)

    // map
    implementation(libs.maps.compose)
    implementation(libs.play.services.maps)
    implementation(libs.material.icons.extended)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.androidx.ui)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Lifecycle Process
    implementation(libs.androidx.lifecycle.process)

    // AdMob & Billing
    implementation("com.google.android.gms:play-services-ads:23.0.0")
    implementation("com.android.billingclient:billing-ktx:6.2.0")

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
