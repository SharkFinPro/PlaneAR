import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

val compileShaders by tasks.registering {
    val shaderSrc = file("src/main/cpp/GraphicsEngine/source/shaders")
    val shaderDst = file("src/main/assets/shaders")

    inputs.dir(shaderSrc)
    outputs.dir(shaderDst)

    doLast {
        shaderDst.mkdirs()

        val shaderFiles = shaderSrc.walkTopDown()
            .filter { f -> f.isFile && f.extension in listOf("vert", "frag", "geom", "comp") }
            .toList()

        val commonInclude = File(shaderSrc, "common")

        println("Found ${shaderFiles.size} shaders")

        shaderFiles.forEach { shader ->
            val outputFile = File(shaderDst, shader.name + ".spv")

            println("Compiling shader: ${shader.relativeTo(shaderSrc)} → ${outputFile.name}")

            exec {
                commandLine(
                    "glslangValidator",
                    "-V",
                    "-I${commonInclude.absolutePath}",
                    shader.absolutePath,
                    "-o",
                    outputFile.absolutePath
                )
            }
        }
    }
}

android {
    namespace = "edu.osu.t22.planear"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "edu.osu.t22.planear"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        externalNativeBuild {
            cmake {
                cppFlags += ""
            }
        }
    }

    buildFeatures {
        prefab = true
    }

    buildTypes {
        release {
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
    kotlinOptions {
        jvmTarget = "11"
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    sourceSets["main"].assets.srcDir("src/main/assets")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.games.activity)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.play.services.location)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test:rules:1.7.0")

}

val vulkanVersion = "1.4.328.1"
val vulkanUrl = "https://github.com/KhronosGroup/Vulkan-ValidationLayers/releases/download/vulkan-sdk-$vulkanVersion/android-binaries-$vulkanVersion.zip"
val downloadDir = layout.buildDirectory.dir("vulkan-layers")
val zipFile = downloadDir.map { it.file("android-binaries.zip") }
val extractDir = layout.projectDirectory.dir("src/main/jniLibs")

tasks.register("downloadVulkanLayers") {
    doLast {
        val zipPath = zipFile.get().asFile
        zipPath.parentFile.mkdirs()
        println("Downloading Vulkan Validation Layers...")
        URL(vulkanUrl).openStream().use { input ->
            Files.copy(input, zipPath.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}

tasks.register<Copy>("extractVulkanLayers") {
    dependsOn("downloadVulkanLayers")
    from(zipTree(zipFile))
    into(extractDir)
}

tasks.named("preBuild") {
    dependsOn("extractVulkanLayers")
}

tasks.matching {it.name.startsWith("merge") && it.name.endsWith("Assets") }
    .configureEach {
        dependsOn(compileShaders)
    }

tasks.matching { it.name.contains("lint", true) }
    .configureEach {
        dependsOn(compileShaders)
    }