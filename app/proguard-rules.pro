# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ─────────────────────────────────────────────────────────────────────────────
# Speedo release(R8) 규칙. Hilt/Room/Billing/Ads/Maps 는 각 라이브러리가 consumer
# proguard 규칙을 동봉하므로 별도 keep 이 대체로 불필요하나, 크래시 디옵스와
# Room/enum 직렬화 안정성을 위한 최소 규칙만 명시한다.
# ─────────────────────────────────────────────────────────────────────────────

# 릴리스 스택트레이스 디옵스용: 라인 번호 보존 + 원본 파일명 은닉.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Room 엔티티/도메인 모델: 필드명이 컬럼/직렬화에 사용되므로 멤버 보존.
-keep class kr.yooreka.speedo.data.local.entity.** { *; }
-keep class kr.yooreka.speedo.domain.model.** { *; }

# enum(LeanConfidence/BrakeEvent/LeanMode 등)의 values()/valueOf() 보존
# (proguard-android-optimize.txt 기본 규칙과 중복이나 명시적으로 고정).
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Kotlin 코루틴 내부 volatile 필드 경고 억제.
-dontwarn kotlinx.coroutines.**