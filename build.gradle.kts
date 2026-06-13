// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.ktlint) apply false
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
}

// 레포에 커밋된 .githooks 를 Git 훅 경로로 등록한다.
// 팀원이 클론 후 어떤 Gradle 태스크든 한 번 실행하면 pre-commit/pre-push 훅이 자동 적용된다.
tasks.register("installGitHooks") {
    description = "Configures git to use the shared .githooks directory."
    group = "git hooks"
    doLast {
        providers.exec {
            commandLine("git", "config", "core.hooksPath", ".githooks")
        }.result.get().assertNormalExitValue()
        logger.lifecycle("✔ git core.hooksPath -> .githooks 설정 완료")
    }
}

// 빌드 시작 시 훅 설치를 자동 트리거한다.
gradle.projectsEvaluated {
    rootProject.tasks.findByName("installGitHooks")?.let { hookTask ->
        subprojects.forEach { sub ->
            sub.tasks.matching { it.name == "preBuild" }.configureEach {
                dependsOn(hookTask)
            }
        }
    }
}