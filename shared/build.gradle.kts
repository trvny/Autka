import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

kotlin {
    android {
        namespace = "com.autka.shared"
        compileSdk = 37
        minSdk = 26
        withHostTestBuilder {}.configure {}
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    val frameworkName = "AutkaShared"
    val xcFramework = XCFramework(frameworkName)
    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = frameworkName
            isStatic = true
            xcFramework.add(this)
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
