import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-library`
    kotlin("jvm") version "2.1.20"
    kotlin("plugin.serialization") version "2.1.20"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.20"
    id("com.gradleup.shadow") version "9.2.2"
}

// =============================================================================
// Project Configuration & Metadata
// =============================================================================

group = "org.hubdustry"

val mindustryVersion = "v160.1"
val mindustryRuntime = configurations.create("mindustryRuntime")

// =============================================================================
// Repositories
// =============================================================================

repositories {
    mavenCentral()
    google()
    maven("https://www.jitpack.io")

    ivy {
        url = uri("https://github.com/")
        metadataSources { artifact() }
        patternLayout {
            artifact("/[organisation]/[module]/releases/download/[revision]/[artifact].jar")
        }
    }
}

// =============================================================================
// Java & Kotlin Compilation Settings
// =============================================================================

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        javaParameters.set(true)
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

// =============================================================================
// Dependencies
// =============================================================================

dependencies {
    // Mindustry Dependencies
    compileOnly("Anuken:Mindustry:$mindustryVersion:dependencies")
    mindustryRuntime("Anuken:Mindustry:$mindustryVersion")

    // JetBrains Annotations
    compileOnly("org.jetbrains:annotations:26.0.1")

    // I/O & Networking
    implementation("com.squareup.okio:okio:3.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // KotlinX Libraries
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")

    // Compose Multiplatform Runtime & Animation
    implementation("org.jetbrains.compose.runtime:runtime:1.7.1")
    implementation("org.jetbrains.compose.animation:animation-core:1.7.1")
    implementation("org.jetbrains.compose.ui:ui-util:1.7.1")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("Anuken:Mindustry:$mindustryVersion:dependencies")
}

// =============================================================================
// Tasks Configuration
// =============================================================================

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

val shadowJar = tasks.named<ShadowJar>("shadowJar") {
    description = "Build desktop mod JAR"
    group = "build"

    archiveFileName.set("${rootProject.name}-desktop.jar")
    mergeServiceFiles()

    from(rootProject.projectDir) {
        include("mod.json")
        include("icon.png")
    }

    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
}

val jarAndroid = tasks.register("jarAndroid") {
    description = "Convert desktop JAR to Android-compatible DEX"
    group = "build"
    dependsOn(shadowJar)

    doLast {
        val androidHome = System.getenv("ANDROID_HOME")
            ?: System.getenv("ANDROID_SDK_ROOT")
            ?: throw GradleException("Set ANDROID_HOME or ANDROID_SDK_ROOT environment variable.")

        val platformRoot = File(androidHome, "platforms").listFiles()
            ?.sortedDescending()
            ?.firstOrNull { File(it, "android.jar").exists() }
            ?: throw GradleException("No android.jar found in Android SDK platforms directory.")

        val buildToolsDir = File(androidHome, "build-tools").listFiles()
            ?.sortedDescending()
            ?.firstOrNull()
            ?: throw GradleException("No build-tools found in Android SDK directory.")

        val isWindows = System.getProperty("os.name").lowercase().contains("windows")
        val d8 = File(buildToolsDir, if (isWindows) "d8.bat" else "d8")
        if (!d8.exists()) {
            throw GradleException("d8 executable not found at: ${d8.absolutePath}")
        }

        val libsDir = file("build/libs")
        val cpFiles = (configurations.compileClasspath.get().files
                + configurations.runtimeClasspath.get().files
                + platformRoot.resolve("android.jar"))
            .filter { it.exists() }

        val cmd = buildList {
            add(d8.absolutePath)
            for (file in cpFiles) {
                add("--classpath")
                add(file.absolutePath)
            }
            addAll(
                listOf(
                    "--min-api", "26",
                    "--output", "${rootProject.name}-android.jar",
                    "${rootProject.name}-desktop.jar"
                )
            )
        }

        val process = ProcessBuilder(cmd)
            .directory(libsDir)
            .inheritIO()
            .start()

        if (process.waitFor() != 0) {
            throw GradleException("d8 dexing failed with exit code ${process.exitValue()}")
        }
    }
}

tasks.register<Jar>("deploy") {
    description = "Merge desktop + android JARs into a single cross-platform mod"
    group = "distribution"
    dependsOn(jarAndroid, shadowJar)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    destinationDirectory.set(file("build/libs"))
    archiveFileName.set("${rootProject.name}.jar")

    from(provider {
        listOf(
            zipTree(file("build/libs/${rootProject.name}-desktop.jar")),
            zipTree(file("build/libs/${rootProject.name}-android.jar"))
        )
    })

    doLast {
        delete(
            file("build/libs/${rootProject.name}-desktop.jar"),
            file("build/libs/${rootProject.name}-android.jar")
        )
    }
}

tasks.register<JavaExec>("runGame") {
    description = "Install mod to Mindustry mods folder and launch game"
    group = "application"
    dependsOn(shadowJar)

    standardInput = System.`in`
    classpath = mindustryRuntime
    mainClass.set("mindustry.desktop.DesktopLauncher")

    doFirst {
        val os = System.getProperty("os.name").lowercase()
        val appData = System.getenv("APPDATA")
        val userHome = System.getProperty("user.home")

        val modsDir = when {
            os.contains("windows") && appData != null -> file("$appData/Mindustry/mods")
            os.contains("mac") -> file("$userHome/Library/Application Support/Mindustry/mods")
            else -> file("$userHome/.local/share/Mindustry/mods")
        }.apply { mkdirs() }

        copy {
            from(shadowJar.get().archiveFile)
            into(modsDir)
        }
        println("Copied mod JAR to: ${modsDir.resolve("${rootProject.name}-desktop.jar")}")
    }
}

tasks.register<JavaExec>("runGameWithoutLoadMod") {
    description = "Launch game without loading mod"
    group = "application"

    standardInput = System.`in`
    classpath = mindustryRuntime
    mainClass.set("mindustry.desktop.DesktopLauncher")
}
