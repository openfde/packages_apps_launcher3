import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

val repoRoot = rootProject.projectDir.absolutePath
val prebuilts = "$repoRoot/gradle-modules/prebuilts"
val generatedDir = "$repoRoot/gradle-modules/generated/app"

android {
    namespace = "com.android.launcher3"
    compileSdk = 37
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.android.launcher3"
        minSdk = 37
        targetSdk = 37
        versionCode = 37
        versionName = "17"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = false
    }

    sourceSets {
        getByName("main") {
            java.setSrcDirs(
                listOf(
                    "$repoRoot/src",
                    "$repoRoot/quickstep/src",
                    "$repoRoot/quickstep/dagger",
                    "$repoRoot/shared/src",
                    "$repoRoot/dagger/src",
                    "$repoRoot/modules/appfunctions/src",
                    "$repoRoot/modules/concurrent/src",
                    "$repoRoot/src_plugins",
                    generatedDir
                )
            )
            res.setSrcDirs(listOf("$repoRoot/res"))
            manifest.srcFile("$repoRoot/gradle-modules/app/AndroidManifest.xml")
        }
        // quickstep/res overlays res/ (higher priority), matching Soong resource order.
        // Uses a patched copy (gradle-modules/tools/sync_res.py) because AGP drops the
        // androidprv namespace and cannot resolve aconfig featureFlag attrs.
        getByName("debug") {
            res.setSrcDirs(listOf("$repoRoot/gradle-modules/generated/quickstep-res"))
        }
        getByName("release") {
            res.setSrcDirs(listOf("$repoRoot/gradle-modules/generated/quickstep-res"))
        }
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }

    androidResources {
        additionalParameters.add("-I")
        additionalParameters.add("$prebuilts/platform/framework-res.apk")
    }

    packaging {
        resources.excludes.addAll(
            listOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/ASL2.0",
                "META-INF/*.kotlin_module"
            )
        )
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        freeCompilerArgs.add("-Xjvm-default=all")
    }
}

val compileOnlyPatterns = listOf(
    "*stubs*",
    "*host*",
    "*annotations*",
    "*jspecify*",
    "*jsr305*",
    "*.impl*",
    "*PlatformMotionTestingComposeValues*",
    "*launcher-quickstep_protolog-groups*",
    "*external_dagger2_dagger2*",
    "*ondeviceintelligence-aconfig*",
    "*WindowManager-Shell-shared-AOSP*",
    "*external_jsr330_jsr330*",
    "*external_jakarta.inject_jakarta.inject*",
    "*personalcontext_ace_common_embeddedscroll*",
    "*guava-listenablefuture*"
)

val jarTree = fileTree("$prebuilts/jars") {
    exclude(
        "*linux_glibc_common*",
        "*local-combined*",
        "*local-javac-header*",
        "*kotlin-stdlib*",
        "*kotlin-annotations*"
    )
}

dependencies {
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
    compileOnly(jarTree.matching { include(compileOnlyPatterns) })
    implementation(jarTree.matching { exclude(compileOnlyPatterns) })
    implementation(fileTree("$prebuilts/aars") { include("*.aar") })

    implementation("com.google.guava:guava:33.2.1-android")
    implementation(project(":widgetpicker"))

    implementation("com.google.dagger:dagger:2.60.1")
    ksp("com.google.dagger:dagger-compiler:2.60.1")
    // Disabled: appfunctions KSP processor stack-overflows with the Maven Kotlin toolchain.\r\n    // AOSP builds it with a custom Kotlin branch. The generated AppFunction metadata is\r\n    // not required for the launcher UI/recents features.\r\n    // ksp("androidx.appfunctions:appfunctions-compiler:1.0.0-alpha08")
}




