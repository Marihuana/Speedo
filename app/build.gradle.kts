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
        versionCode = 6
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // AndroidManifest의 ${MAPS_API_KEY} 자리에 주입됩니다.
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey

        // 광고 노출 여부(마스터 스위치). 광고 단위 ID 는 BannerAd/AdManager 에서 debug=구글 테스트 ID,
        // release=프로덕션 ID 로 분기하므로, debug 에서 광고를 켜도 테스트 광고만 노출되어 정책 위반이 없다.
        buildConfigField("boolean", "ADS_ENABLED", "true")
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

    // Firebase Crashlytics (google-services.json 존재 시 플러그인이 적용되어 활성화됨)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)

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
