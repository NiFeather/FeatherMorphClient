plugins {
    id("fabric-loom") version "1.10.4"
    id("maven-publish")
}

version = project.property("mod_version") as String
group = project.property("maven_group") as String

repositories {
    mavenLocal()
    maven { url = uri("https://maven.shedaniel.me/") }
    maven { url = uri("https://maven.terraformersmc.com/") }
    maven { url = uri("https://jitpack.io") }

    exclusiveContent {
        forRepository {
            maven {
                name = "Modrinth"
                url = uri("https://api.modrinth.com/maven")
            }
        }
        filter {
            includeGroup("maven.modrinth")
        }
    }
}

tasks.processResources {
    val replaces = mapOf(
        Pair("version", project.version),
        Pair("mc_version", project.property("minecraft_version")),
        Pair("loader_version", project.property("loader_version")),
        Pair("clothconfig_version", project.property("clothconfig_version"))
    );

    inputs.properties(replaces);

    filesMatching("fabric.mod.json") {
        expand(replaces);
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")
    //mappings("net.fabricmc:yarn:${project.property("yarn_mappings")}:v2")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:${project.property("loader_version")}")

    //modApi("com.terraformersmc:modmenu:${project.property("modmenu_version")}")
    modApi("me.shedaniel.cloth:cloth-config-fabric:${project.property("clothconfig_version")}") {
        exclude(group = "net.fabricmc.fabric-api")
    }

    val protocolVersion = if (project.property("protocols_use_local_build") == "true") {
        project.property("protocols_local_version") as String
    } else {
        project.property("protocols_version") as String
    }

    //modApi("maven.modrinth:entity-model-features:2.4.3")
    modImplementation("com.github.XiaMoZhiShi:feathermorph-protocols:$protocolVersion")
    modImplementation("me.shedaniel.cloth:cloth-config-fabric:${project.property("clothconfig_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${project.property("fabric_version")}")

    include("com.github.XiaMoZhiShi:feathermorph-protocols:$protocolVersion")
}

loom {
    accessWidenerPath = file("src/main/resources/morphclient.accesswidener")
}

tasks.withType<JavaCompile> {
    // ensure that the encoding is set to UTF-8, no matter what the system default is
    // this fixes some edge cases with special characters not displaying correctly
    // see http://yodaconditions.net/blog/fix-for-java-file-encoding-problems-with-gradle.html
    // If Javadoc is generated, this must be specified in that task too.
    options.encoding = "UTF-8"
    if (JavaVersion.current().isJava10Compatible()) {
        options.release = 21
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    //archiveBaseName.set(project.property("archives_base_name") as String)
    withSourcesJar()
}

tasks.withType<Jar> {
    archiveBaseName.set(project.property("archives_base_name") as String)
}

tasks.named<Jar>("jar") {
    from("LICENSE") {
        rename { "${it}_${project.property("archives_base_name")}" }
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }

    repositories {
        // Add repositories to publish to here.
        // Notice: This block does NOT have the same function as the block in the top level.
        // The repositories here will be used for publishing your artifact, not for
        // retrieving dependencies.
    }
}
