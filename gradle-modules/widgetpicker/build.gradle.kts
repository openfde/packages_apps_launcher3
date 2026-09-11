import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

val repoRoot = rootProject.projectDir.absolutePath
val prebuilts = "$repoRoot/gradle-modules/prebuilts"

android {
    namespace = "com.android.launcher3.widgetpicker"
    compileSdk = 37
    buildToolsVersion = "36.0.0"

    defaultConfig {
        minSdk = 31
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
    }

    sourceSets {
        getByName("main") {
            java.setSrcDirs(listOf("$repoRoot/modules/widgetpicker/src"))
            res.setSrcDirs(listOf("$repoRoot/modules/widgetpicker/res"))
            manifest.srcFile("$repoRoot/gradle-modules/widgetpicker/AndroidManifest.xml")
        }
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        freeCompilerArgs.add("-Xjvm-default=all")
    }
}

dependencies {
    implementation(fileTree("$prebuilts/aars") { include("androidx_*.aar") })
    implementation(
        fileTree("$prebuilts/jars") {
            exclude(
                "*linux_glibc_common*",
                "*local-combined*",
                "*local-javac-header*",
                "*kotlin-stdlib*",
                "*kotlin-annotations*"
            )
            include(
                "*androidx_*",
                "*kotlin-parcelize*",
                "*coroutines*"
            )
        }
    )
    implementation("com.google.dagger:dagger:2.60.1")
    ksp("com.google.dagger:dagger-compiler:2.60.1")
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
}

