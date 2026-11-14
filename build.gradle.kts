import net.ltgt.gradle.errorprone.errorprone

import java.net.URI
import java.security.MessageDigest

plugins {
    id("net.fabricmc.fabric-loom-remap") version "1.17-SNAPSHOT"
    id("ploceus") version "1.17.4"
    id("net.ltgt.errorprone") version "5.1.1"
    id("com.diffplug.spotless") version "8.11.0"
}

val modid = property("mod.id") as String
val shimStubsJar = layout.buildDirectory.file("flashback/compile-stubs.jar")
val flashbackCompileDir = layout.buildDirectory.dir("flashback/compile")
val flashbackJar = layout.projectDirectory.file(
    "run/flashback/Flashback-${property("flashback.version")}-for-MC${property("flashback.mc")}.jar"
)

version = "${property("mod.version")}+${property("minecraft_version")}"
group = property("mod.group") as String
base.archivesName = modid

ploceus {
    setIntermediaryGeneration(2)
}

repositories {
    mavenCentral()
    maven("https://maven.cloverclient.com/releases") {
        content { includeGroup("pl.tomgirl") }
    }
}

configurations.configureEach {
    exclude(group = "org.lwjgl.lwjgl")
}

val patcher = sourceSets.create("patcher")

dependencies {
    "patcherCompileOnly"("org.ow2.asm:asm-tree:9.10.1")
    "patcherCompileOnly"("org.ow2.asm:asm-commons:9.10.1")
    implementation(patcher.output)
    compileOnly(files(shimStubsJar).builtBy("compileStubs"))
    compileOnly(files(flashbackCompileDir.map { it.file("flashback-patched.jar") }).builtBy("patchFlashback"))

    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings(ploceus.layeredMappings {
        mappings("net.ornithemc:feather-gen2:${property("minecraft_version")}+build.${property("feather_build")}:v2")
    })
    ploceus.dependOsl(property("osl_version") as String)

    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    modImplementation("pl.tomgirl:pylon:${property("pylon_version")}")

    include(implementation("org.joml:joml:1.10.9")!!)
    include(implementation("com.mojang:datafixerupper:10.0.21") { isTransitive = false })

    errorprone("com.google.errorprone:error_prone_core:2.51.0")
}

loom {
    runConfigs.all {
        runDirectory = file("run")
        jvmArguments.add("-Dmixin.debug.export=true")
    }
    runConfigs.remove(runConfigs["server"])
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks.withType<JavaCompile>().configureEach {
    options.apply {
        encoding = "UTF-8"
        release = 25

        errorprone {
            disableAllChecks = true
            error("UnnecessarilyFullyQualified")
        }
    }
    finalizedBy("spotlessJavaCheck")
}

spotless {
    java {
        importOrder("", "javax|java", "\\#")
        removeUnusedImports()
    }
}

tasks.processResources {
    val props = mapOf(
        "mod_id" to modid,
        "mod_name" to project.property("mod.name"),
        "mod_version" to project.version,
        "loader_version" to project.property("loader_version"),
        "flashback_version" to project.property("flashback.version"),
        "flashback_mc" to project.property("flashback.mc"),
        "flashback_sha1" to project.property("flashback.sha1"),
    )
    inputs.properties(props)
    filesMatching("fabric.mod.json") { expand(props) }
}

val downloadFlashback = tasks.register("downloadFlashback") {
    description = "Downloads the targeted Flashback jar into run/ for development."
    val url = project.property("flashback.url") as String
    val sha1 = project.property("flashback.sha1") as String
    val out = flashbackJar
    outputs.file(out)
    doLast {
        val file = out.asFile
        if (!file.exists()) {
            file.parentFile.mkdirs()
            URI(url).toURL().openStream().use { input -> file.outputStream().use { input.copyTo(it) } }
        }
        val actual = MessageDigest.getInstance("SHA-1").digest(file.readBytes()).joinToString("") { "%02x".format(it) }
        check(actual == sha1) { "Flashback jar SHA-1 mismatch: expected $sha1, got $actual" }
    }
}

tasks.register<JavaExec>("flashbackReport") {
    group = "flashback"
    description = "Patches the Flashback jar and lists every reference that does not resolve on 1.8.9."
    dependsOn(downloadFlashback, tasks.classes)
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "org.polyfrost.flashbacklegacy.patch.FlashbackReport"
    args(flashbackJar.asFile.absolutePath, layout.buildDirectory.file("flashback-report.md").get().asFile.absolutePath)
    args((project.findProperty("trace") as String? ?: "com/moulberry/flashback/Flashback.startRecordingReplay()V").split(","))
}

tasks.jar { from(patcher.output) }

val modernMinecraftJar = layout.buildDirectory.file("flashback/minecraft-${property("flashback.mc")}-client.jar")
val downloadModernMinecraft = tasks.register("downloadModernMinecraft") {
    description = "Downloads the Minecraft client Flashback targets; only its class hierarchy is used."
    val version = project.property("flashback.mc") as String
    val out = modernMinecraftJar
    outputs.file(out)
    doLast {
        val file = out.get().asFile
        if (file.exists()) return@doLast
        fun json(url: String) = groovy.json.JsonSlurper().parse(URI(url).toURL()) as Map<*, *>
        val manifest = json("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json")
        val entry = (manifest["versions"] as List<*>).map { it as Map<*, *> }.first { it["id"] == version }
        val client = ((json(entry["url"] as String)["downloads"] as Map<*, *>)["client"]) as Map<*, *>
        file.parentFile.mkdirs()
        URI(client["url"] as String).toURL().openStream().use { input -> file.outputStream().use { input.copyTo(it) } }
    }
}

val shimHierarchyDir = layout.buildDirectory.dir("generated/shimHierarchy")
val shimHierarchy = tasks.register<JavaExec>("shimHierarchy") {
    group = "flashback"
    description = "Records the Minecraft class hierarchy of every type Flashback references, for runtime stubs."
    dependsOn(downloadFlashback, downloadModernMinecraft)
    classpath = patcher.output + patcher.compileClasspath
    mainClass = "org.polyfrost.flashbacklegacy.patch.ShimStubs"
    val out = shimHierarchyDir.map { it.file("flashback_legacy/shim-hierarchy.txt") }
    inputs.file(flashbackJar)
    inputs.file(modernMinecraftJar)
    outputs.dir(shimHierarchyDir)
    args(flashbackJar.asFile.absolutePath, modernMinecraftJar.get().asFile.absolutePath, out.get().asFile.absolutePath)
}
sourceSets.main { resources.srcDir(shimHierarchy) }

val compileStubs = tasks.register<JavaExec>("compileStubs") {
    description = "Stubs the shimmed classes that have no hand-written source yet, for compiling against."
    val hierarchy = shimHierarchyDir.map { it.file("flashback_legacy/shim-hierarchy.txt") }
    val sources = layout.projectDirectory.dir("src/main/java/org/polyfrost/flashbacklegacy/shim")
    dependsOn(shimHierarchy)
    classpath = patcher.output + patcher.compileClasspath
    mainClass = "org.polyfrost.flashbacklegacy.patch.ShimStubs\$Compile"
    inputs.dir(shimHierarchyDir)
    inputs.files(fileTree(sources))
    outputs.file(shimStubsJar)
    args(hierarchy.get().asFile.absolutePath, shimStubsJar.get().asFile.absolutePath, layout.projectDirectory.dir("src/main/java").asFile.absolutePath)
}

tasks.register<JavaExec>("patchFlashback") {
    description = "Patches the Flashback jar so this mod's mixins into Flashback can compile against it."
    dependsOn(downloadFlashback)
    classpath = patcher.output + patcher.compileClasspath
    mainClass = "org.polyfrost.flashbacklegacy.patch.FlashbackPatcher"
    inputs.file(flashbackJar)
    outputs.dir(flashbackCompileDir)
    args(flashbackJar.asFile.absolutePath, flashbackCompileDir.get().asFile.absolutePath)
}

tasks.named("runClient") { dependsOn(downloadFlashback) }
