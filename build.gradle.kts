import java.util.zip.ZipFile

plugins {
    idea
    `maven-publish`
    jacoco
    id("net.minecraftforge.gradle") version "6.0.54"
    id("org.parchmentmc.librarian.forgegradle") version "1.2.0"
    id("org.spongepowered.mixin") version "0.7.38"
}

mixin {
    add(sourceSets.main.get(), "better_discovery_guides.refmap.json")
    config("better_discovery_guides.mixins.json")
}

group = "com.bettercontent"
version = property("mod_version") as String
base {
    archivesName.set(property("artifact_name") as String)
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

// Isolated real-client review; never included in the runtime JAR.
val learningVisual by sourceSets.creating {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}
configurations[learningVisual.implementationConfigurationName].extendsFrom(configurations.implementation.get())
configurations[learningVisual.runtimeOnlyConfigurationName].extendsFrom(configurations.runtimeOnly.get())
// Full-pack provider routes need their real installed mods and run in pack integration.
// Compile their scenarios here without registering them in the isolated GameTest server.
val packIntegration by sourceSets.creating {
    compileClasspath += sourceSets.main.get().output + configurations.compileClasspath.get()
}

minecraft {
    mappings("official", property("minecraft_version") as String)
    copyIdeResources = true
    jarJar.enable()

    runs {
        configureEach {
            workingDirectory(project.file("run"))
            property("forge.logging.console.level", "debug")
            property("mixin.env.remapRefMap", "true")
            property("mixin.env.refMapRemappingFile", "${projectDir}/build/createSrgToMcp/output.srg")
            mods {
                create(property("mod_id") as String) {
                    source(sourceSets.main.get())
                }
            }
        }
        val baseClient = create("client")
        create("learningVisual") {
            parent(baseClient)
            args("--mixin", "better_discovery_guides.visual.mixins.json")
            workingDirectory(project.file("build/learning-visual"))
            args("--width", "1280", "--height", providers.gradleProperty("learningVisualHeight").orElse("720").get())
            mods { getByName(property("mod_id") as String).source(learningVisual) }
        }
        create("server") { arg("--nogui") }
        create("gameTestServer") {
            workingDirectory(project.file("run-gametest"))
            property("forge.enableGameTest", "true")
            property("forge.gameTestServer", "true")
            property("forge.enabledGameTestNamespaces", property("mod_id") as String)
            arg("--nogui")
        }
    }
}

// CI and fresh-release builds provide verified runtime JARs explicitly.
// Ordinary local builds retain the canonical sibling build/libs convention.
fun betterContentJar(repository: String, artifact: String): java.io.File {
    val directory = providers.environmentVariable("BC_CUSTOM_MOD_JAR_DIR").orNull
    require(directory == null || directory.isNotBlank()) { "BC_CUSTOM_MOD_JAR_DIR must not be blank" }
    val jar = if (directory == null) file("../$repository/build/libs/$artifact") else file(directory).resolve(artifact)
    require(jar.isFile) {
        "Missing Better Content provider $artifact at $jar; prepare BC_CUSTOM_MOD_JAR_DIR or build $repository first"
    }
    return jar
}

repositories {
    flatDir { dirs("../better-deaths-door/build/libs") }
    maven("https://api.modrinth.com/maven") { content { includeGroup("maven.modrinth") } }
    maven("https://maven.minecraftforge.net")
    maven("https://harleyoconnor.com/maven")
    maven("https://repo.spongepowered.org/repository/maven-public/")
    maven("https://maven.llamalad7.mixinextras.org/releases/")
    maven("https://maven.createmod.net")
    maven("https://maven.valkyrienskies.org") { content { includeGroup("org.valkyrienskies.core") } }
    maven("https://www.cursemaven.com") { content { includeGroup("curse.maven") } }
    mavenCentral()
}

val betterContentApiJars = files(
    betterContentJar("better-deaths-door", "better-deaths-door-1.0.0.jar"),
    betterContentJar("better-pillager-campaigns", "better-pillager-campaigns-0.5.4.jar"),
    betterContentJar("better-world-management", "better-world-management-0.1.0.jar"),
    betterContentJar("better-dimension-fonts", "better-dimension-fonts-1.0.0.jar"),
    betterContentJar("better-rpg-progression", "better-rpg-progression-1.0.1.jar"),
    betterContentJar("better-magic-chunk-anchors", "better-magic-chunk-anchors-0.1.0.jar"),
    betterContentJar("better-spirit-commerce", "better-spirit-commerce-1.0.1.jar"),
    betterContentJar("better-industrial-heat", "better-industrial-heat-0.1.0.jar"),
    betterContentJar("better-settlement-roads", "better-settlement-roads-0.1.0.jar"),
    betterContentJar("better-drinking-water", "better-drinking-water-1.1.0.jar"),
    betterContentJar("better-compat-fixes", "better-compat-fixes-0.1.9.jar"),
    betterContentJar("better-player-traces", "better-player-traces-0.1.0.jar"),
    betterContentJar("better-survival-physiology", "better-survival-physiology-0.1.1.jar"),
    betterContentJar("better-ore-geology", "better-ore-geology-0.3.0.jar"),
    betterContentJar("better-chemlib-hazards", "better-chemlib-hazards-0.2.0.jar"),
    betterContentJar("better-cave-encounters", "better-cave-encounters-0.2.1.jar"),
    betterContentJar("better-bumblezone-crops", "better-bumblezone-crops-0.1.0.jar"),
    betterContentJar("better-tinkers-loot-affixes", "better-tinkers-loot-affixes-1.0.0.jar"),
    betterContentJar("better-create-kinetic-loss", "better-create-kinetic-loss-0.1.0.jar"),
    betterContentJar("better-create-train-fuel", "better-create-train-fuel-0.1.0.jar"),
    betterContentJar("better-rail-beetle", "better-rail-beetle-0.1.0.jar"),
    betterContentJar("better-oc2r-create-controls", "better-oc2r-create-controls-0.1.0.jar"),
    betterContentJar("better-oc2r-wireless-messaging", "better-oc2r-wireless-messaging-1.0.0.jar")
)

dependencies {
    minecraft("net.minecraftforge:forge:${property("minecraft_version")}-${property("forge_version")}")
    compileOnly(files(betterContentJar("better-gameplay-notices", "better-gameplay-notices-1.0.0.jar")))
    runtimeOnly(files(betterContentJar("better-gameplay-notices", "better-gameplay-notices-1.0.0.jar")))
    add(learningVisual.compileOnlyConfigurationName, files(betterContentJar("better-deaths-door", "better-deaths-door-1.0.0.jar")))
    if (providers.gradleProperty("combinedDeathVisual").map(String::toBoolean).getOrElse(false)) {
        add(learningVisual.runtimeOnlyConfigurationName, fg.deobf("com.bettercontent:better-deaths-door:1.0.0"))
    }
    compileOnly(betterContentApiJars)
    testCompileOnly(betterContentApiJars)
    testRuntimeOnly(betterContentApiJars)
    testRuntimeOnly("org.jetbrains.kotlin:kotlin-stdlib:2.0.0")
    compileOnly(fg.deobf("curse.maven:hyle-609850:7736352"))
    compileOnly(fg.deobf("curse.maven:diet-443570:4813904"))
    compileOnly(fg.deobf("curse.maven:occultism-361026:7429593"))
    compileOnly(fg.deobf("curse.maven:malum-484064:6646111"))
    compileOnly(fg.deobf("curse.maven:thirst-was-taken-679270:6660408"))
    compileOnly(fg.deobf("curse.maven:cold-sweat-506194:7893262"))
    compileOnly(fg.deobf("curse.maven:pollution-of-the-realms-269973:8554528"))
    compileOnly(fg.deobf("curse.maven:little-logistics-570050:4799459"))
    compileOnly(fg.deobf("curse.maven:weather-storms-tornadoes-237746:5244118"))
    compileOnly(fg.deobf("curse.maven:creativecore-257814:7649757"))
    compileOnly(fg.deobf("curse.maven:ambientsounds-254284:7550220"))
    compileOnly(fg.deobf("curse.maven:oculus-581495:6020952"))
    compileOnly(fg.deobf("curse.maven:sophisticated-core-618298:7916595"))
    compileOnly(fg.deobf("curse.maven:sophisticated-storage-619320:7973265"))
    compileOnly(fg.deobf("curse.maven:curios-api-309927:6418456"))
    compileOnly(fg.deobf("curse.maven:mantle-74924:7563777"))
    compileOnly(fg.deobf("curse.maven:tinkers-construct-74072:7449219"))
    compileOnly(fg.deobf("curse.maven:pneumaticcraft-repressurized-281849:7307654"))
    compileOnly(fg.deobf("maven.modrinth:create-sifting:1.20.1-1.8.6-6.0.6"))
    compileOnly(fg.deobf("curse.maven:ars-nouveau-401955:6688854"))
    compileOnly(fg.deobf("curse.maven:blood-magic-224791:7956981"))
    compileOnly(fg.deobf("curse.maven:goety-586095:8087429"))
    compileOnly(fg.deobf("curse.maven:applied-energistics-2-223794:7148487"))
    compileOnly(fg.deobf("curse.maven:ars-energistique-905641:5504444"))
    compileOnly(fg.deobf("curse.maven:create-creating-space-858897:7850072"))
    compileOnly(fg.deobf("curse.maven:power-grid-1321420:7714613"))
    compileOnly(fg.deobf("com.simibubi.create:create-${property("minecraft_version")}:6.0.8-291:slim"))
    compileOnly(fg.deobf("com.ferreusveritas.dynamictrees:DynamicTrees-1.20.1:1.4.10"))
    compileOnly(fg.deobf("curse.maven:serene-seasons-291874:6398227"))
    compileOnly(fg.deobf("curse.maven:polymorph-388800:6450982"))
    compileOnly(fg.deobf("curse.maven:architectury-api-419699:5137938"))
    compileOnly(fg.deobf("curse.maven:ftb-library-forge-404465:7296748"))
    compileOnly(fg.deobf("curse.maven:ftb-teams-forge-404468:7499810"))
    compileOnly(fg.deobf("curse.maven:emi-580555:8081375"))
    compileOnly(fg.deobf("net.createmod.ponder:Ponder-Forge-1.20.1:1.0.92"))
    compileOnly(fg.deobf("curse.maven:epic-fight-mod-405076:8049910"))
    compileOnly(fg.deobf("curse.maven:valkyrien-skies-258371:7906689"))
    compileOnly(fg.deobf("curse.maven:relics-445274:7708970"))
    compileOnly(fg.deobf("curse.maven:realistic-block-physics-375616:6393411"))
    compileOnly(fg.deobf("curse.maven:realistic-physics-1030082:6026115"))
    compileOnly(fg.deobf("curse.maven:rehooked-1096531:6341096"))
    testRuntimeOnly(fg.deobf("curse.maven:rehooked-1096531:6341096"))
    compileOnly(fg.deobf("curse.maven:patchouli-306770:7731017"))
    compileOnly("org.valkyrienskies.core:api:1.1.0+cf208d8b56")
    annotationProcessor("org.spongepowered:mixin:0.8.5:processor")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("com.google.code.gson:gson:2.10.1")
}

tasks.named<Jar>("jar") {
    dependsOn(tasks.named("compileJava"))
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(layout.buildDirectory.file("tmp/compileJava/compileJava-refmap.json")) {
        rename { "better_discovery_guides.refmap.json" }
    }
    finalizedBy("reobfJar")
}

val stageRuntimeJar by tasks.registering(Copy::class) {
    group = "build"
    description = "Stages the reobfuscated runtime jar into build/libs using the canonical release filename."
    dependsOn(tasks.named("reobfJar"))
    mustRunAfter(tasks.named("jarJar"))
    mustRunAfter(tasks.named("reobfJarJar"))
    from(layout.buildDirectory.file("reobfJar/output.jar"))
    into(layout.buildDirectory.dir("libs"))
    rename { "${base.archivesName.get()}-$version.jar" }
}

tasks.named("assemble") {
    dependsOn(stageRuntimeJar)
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
}

tasks.named<JavaCompile>("compileJava") {
    outputs.file(layout.buildDirectory.file("tmp/compileJava/compileJava-refmap.json"))
}

tasks.test {
    doFirst { systemProperty("better_discovery_guides.optionalCompileClasspath", configurations.compileClasspath.get().asPath) }
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

val verifyNoReflection by tasks.registering {
    group = "verification"
    description = "Rejects Java/Kotlin source that uses runtime reflection."
    val sourceTree = fileTree("src") {
        include("**/*.java", "**/*.kt", "**/*.kts")
    }
    inputs.files(sourceTree)
    doLast {
        val forbidden = listOf(
            "java.lang.reflect", "kotlin.reflect", "Class.forName(",
            ".getDeclaredField(", ".getDeclaredMethod(", ".getDeclaredConstructor(",
            ".getField(", ".getMethod(", ".setAccessible(", ".trySetAccessible(",
            "Proxy.newProxyInstance(", "MethodHandles", "VarHandle", "sun.misc.Unsafe"
        )
        val violations = sourceTree.files.sorted().flatMap { source ->
            source.readLines().mapIndexedNotNull { index, line ->
                forbidden.firstOrNull(line::contains)?.let { token ->
                    "${source.relativeTo(projectDir)}:${index + 1}: $token"
                }
            }
        }
        check(violations.isEmpty()) {
            "Runtime reflection is forbidden:\n${violations.joinToString("\n")}"
        }
    }
}

tasks.named("check") {
    dependsOn(verifyNoReflection)
}

tasks.register("headlessGameTest") {
    group = "verification"
    description = "Runs Forge game tests in a headless dedicated server."
    dependsOn(tasks.named("runGameTestServer"))
}

tasks.register("verifyFast") {
    group = "verification"
    description = "Runs deterministic unit/resource checks without Forge game tests."
    dependsOn(tasks.named("check"))
    dependsOn(tasks.named("compilePackIntegrationJava"))
}

tasks.register("verifyFull") {
    group = "verification"
    description = "Runs the full verification lane including headless Forge game tests."
    dependsOn(tasks.named("verifyFast"))
    dependsOn(tasks.named("headlessGameTest"))
    dependsOn("verifyRuntimeJar")
}

val verifyRuntimeJar by tasks.registering {
    group = "verification"
    description = "Rejects a runtime JAR with a missing LevelLoadingScreen accessor refmap."
    dependsOn(stageRuntimeJar)

    val runtimeJar = layout.buildDirectory.file("libs/${base.archivesName.get()}-$version.jar")
    inputs.file(runtimeJar)

    doLast {
        ZipFile(runtimeJar.get().asFile).use { zip ->
            val refmap = zip.getEntry("better_discovery_guides.refmap.json")
                ?: throw GradleException("Runtime JAR is missing better_discovery_guides.refmap.json")
            val refmapText = zip.getInputStream(refmap).bufferedReader().use { it.readText() }
            check(refmapText.contains("LevelLoadingScreenAccessor") &&
                refmapText.contains("\"progressListener\": \"f_96138_")) {
                "Runtime refmap lacks the LevelLoadingScreen progress-listener accessor mapping"
            }
        }
    }
}

tasks.processResources {
    val props = mapOf(
        "minecraft_version" to project.property("minecraft_version"),
        "forge_version" to project.property("forge_version"),
        "mod_id" to project.property("mod_id"),
        "mod_name" to project.property("mod_name"),
        "mod_version" to project.property("mod_version")
    )
    inputs.properties(props)
    filesMatching(listOf("META-INF/mods.toml", "pack.mcmeta")) {
        expand(props)
    }
}

apply(from = "gradle/gametest-evidence.gradle")
