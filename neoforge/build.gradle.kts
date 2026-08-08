import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    id("java-library")
    id("net.neoforged.moddev") version "2.0.91"
    kotlin("jvm") version "2.2.20"
}

fun prop(name: String) = property(name) as String

val minecraftVersion = prop("minecraft_version")
val minecraftVersionRange = prop("minecraft_version_range")
val neoVersion = prop("neo_version")
val neoVersionRange = prop("neo_version_range")
val loaderVersionRange = prop("loader_version_range")
val kffVersion = prop("kff_version")
val kffVersionRange = prop("kff_version_range")
val parchmentMinecraftVersion = prop("parchment_minecraft_version")
val parchmentMappingsVersion = prop("parchment_mappings_version")
val modId = prop("mod_id")
val modName = prop("mod_name")
val modLicense = prop("mod_license")
val modVersion = prop("mod_version")
val modGroupId = prop("mod_group_id")
val modAuthors = prop("mod_authors")

version = modVersion
group = modGroupId
base.archivesName = modId

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
        // ATM10 が同梱する KFF 5.x の Kotlin stdlib (2.0 系) より新しい API を
        // 誤って使わないよう、言語/API レベルを 2.0 に固定する。
        apiVersion = KotlinVersion.KOTLIN_2_0
        languageVersion = KotlinVersion.KOTLIN_2_0
    }
}

repositories {
    mavenLocal()
    maven("https://thedarkcolour.github.io/KotlinForForge/") {
        name = "Kotlin for Forge"
        content { includeGroup("thedarkcolour") }
    }
}

dependencies {
    // Kotlin stdlib / 言語ローダーは実行時に KFF が提供する。
    // MDG は依存を JAR に同梱しないため、配布 JAR は本MODのクラスのみになる。
    implementation("thedarkcolour:kotlinforforge-neoforge:$kffVersion")
}

neoForge {
    version = neoVersion

    parchment {
        mappingsVersion = parchmentMappingsVersion
        minecraftVersion = parchmentMinecraftVersion
    }

    runs {
        configureEach {
            logLevel = org.slf4j.event.Level.DEBUG
        }
        // クライアント側の追加を必要としない設計のため、動作確認はサーバーで行う。
        create("server") {
            server()
            programArgument("--nogui")
        }
        create("client") {
            client()
        }
    }

    mods {
        create(modId) {
            sourceSet(sourceSets.main.get())
        }
    }
}

val generateModMetadata by tasks.registering(ProcessResources::class) {
    val replaceProperties = mapOf(
        "minecraft_version" to minecraftVersion,
        "minecraft_version_range" to minecraftVersionRange,
        "neo_version" to neoVersion,
        "neo_version_range" to neoVersionRange,
        "loader_version_range" to loaderVersionRange,
        "kff_version_range" to kffVersionRange,
        "mod_id" to modId,
        "mod_name" to modName,
        "mod_license" to modLicense,
        "mod_version" to modVersion,
        "mod_authors" to modAuthors,
    )
    inputs.properties(replaceProperties)
    filteringCharset = "UTF-8"
    expand(replaceProperties)
    from("src/main/templates")
    into(layout.buildDirectory.dir("generated/sources/modMetadata"))
}

sourceSets.main.get().resources.srcDir(generateModMetadata)
neoForge.ideSyncTask(generateModMetadata)

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.jar {
    manifest {
        attributes("Implementation-Version" to modVersion)
    }
}
