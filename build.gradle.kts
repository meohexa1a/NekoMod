plugins {
    `java-library`
    kotlin("jvm") version "2.1.20"
    kotlin("plugin.serialization") version "2.1.20"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.20"
    id("com.gradleup.shadow") version "9.2.2"
}

group = "org.mdt"

repositories {
    mavenCentral()
    maven("https://maven.google.com")
    maven("https://www.jitpack.io")

    ivy {
        url = uri("https://github.com/")
        metadataSources { artifact() }
        patternLayout {
            artifact("/[organisation]/[module]/releases/download/[revision]/[artifact].jar")
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        javaParameters = true
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

val mindustryVersion = "v159.7"
val mindustryRuntime = configurations.create("mindustryRuntime")

dependencies {
    compileOnly("Anuken:Mindustry:$mindustryVersion:dependencies")
    mindustryRuntime("Anuken:Mindustry:$mindustryVersion")

    compileOnly("org.jetbrains:annotations:26.0.1")

    implementation("org.codehaus.janino:janino:3.1.12")
    implementation("com.squareup.okio:okio:3.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
    implementation("org.jetbrains.compose.runtime:runtime-desktop:1.7.1")
    implementation("org.jetbrains.compose.animation:animation-core-desktop:1.7.1")
    implementation("org.dom4j:dom4j:2.1.4")

    testImplementation(kotlin("test"))
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    description = "Build desktop mod JAR"
    from(rootProject.projectDir) {
        include("mod.json")
        include("icon.png")
    }

    archiveFileName.set("${rootProject.name}-desktop.jar")
    mergeServiceFiles()
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
}

val shadowJar = tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar")

tasks.register("jarAndroid") {
    description = "Convert desktop JAR to Android-compatible DEX"
    dependsOn("shadowJar")
    doLast {
        val androidHome = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
        ?: throw GradleException("Set ANDROID_HOME or ANDROID_SDK_ROOT")
        val platformRoot = File(androidHome, "platforms").listFiles()
            ?.sorted()?.reversed()
            ?.firstOrNull { File(it, "android.jar").exists() }
            ?: throw GradleException("No android.jar found in SDK platforms")
        val buildToolsDir = File(androidHome, "build-tools").listFiles()
            ?.sorted()?.reversed()?.firstOrNull()
            ?: throw GradleException("No build-tools found")
        val d8 = if (System.getProperty("os.name").lowercase().contains("windows"))
            File(buildToolsDir, "d8.bat") else File(buildToolsDir, "d8")
        if (!d8.exists()) throw GradleException("d8 not found at ${d8.absolutePath}")

        val libs = file("build/libs")
        val cp = (configurations.compileClasspath.get().files
                + configurations.runtimeClasspath.get().files
                + platformRoot.resolve("android.jar"))
            .joinToString(" ") { "--classpath ${it.absolutePath}" }
        val proc = ProcessBuilder(
            d8.absolutePath, *cp.split(" ").toTypedArray(),
            "--min-api", "26", "--output", "${rootProject.name}-android.jar",
            "${rootProject.name}-desktop.jar"
        )
            .directory(libs).inheritIO().start()
        if (proc.waitFor() != 0) throw GradleException("d8 failed")
    }
}

tasks.register<Jar>("deploy") {
    description = "Merge desktop + android JARs into a single cross-platform mod"
    dependsOn("jarAndroid", "shadowJar")
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
        delete("build/libs/${rootProject.name}-desktop.jar", "build/libs/${rootProject.name}-android.jar")
    }
}

tasks.register<JavaExec>("runGame") {
    description = "Install mod to Mindustry mods folder and launch game"
    dependsOn(shadowJar)
    doFirst {
        val modsDir = file(System.getenv("APPDATA") + "/Mindustry/mods").also { it.mkdirs() }
        copy {
            from(shadowJar.get().archiveFile)
            into(modsDir)
        }
        println("Copied to " + modsDir.resolve("${rootProject.name}-desktop.jar"))
    }
    standardInput = System.`in`
    classpath = mindustryRuntime
    mainClass.set("mindustry.desktop.DesktopLauncher")
}
