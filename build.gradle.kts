import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("gg.grounds.base-conventions") version "0.8.0"
    kotlin("jvm") version "2.2.20"
    `maven-publish`
}

group = "gg.grounds"

// Toolchain 25 (CI ships JDK 25) but bytecode targets Java 21 -- this library is
// consumed by the Velocity/Paper *plugins*, whose Gradle compileClasspath pins
// org.gradle.jvm.version to a level below 25. A library published at jvm.version 25
// is rejected by those consumers ("only compatible with JVM runtime version 25 or
// newer"). 21 is an LTS floor every consumer (plugins and the Java 25 services)
// accepts, and this library uses no API newer than Java 9.
kotlin { jvmToolchain(25) }

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    withSourcesJar()
}

tasks.withType<KotlinCompile>().configureEach { compilerOptions.jvmTarget.set(JvmTarget.JVM_21) }

repositories { mavenCentral() }

// Compiled against the oldest Adventure we still support. Paper, Velocity and the
// Minestom runtime all ship their own newer copy, and a plugin must use *that* one --
// a shaded Adventure gives you a second Component type that sendMessage rejects.
// Compiling low and running high is safe; the reverse is not.
val adventureVersion = "4.17.0"

dependencies {
    compileOnly("net.kyori:adventure-api:$adventureVersion")
    compileOnly("net.kyori:adventure-text-minimessage:$adventureVersion")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("net.kyori:adventure-api:$adventureVersion")
    testImplementation("net.kyori:adventure-text-minimessage:$adventureVersion")
    testImplementation("net.kyori:adventure-text-serializer-plain:$adventureVersion")
}

tasks.test { useJUnitPlatform() }

publishing {
    publications { create<MavenPublication>("maven") { from(components["java"]) } }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/groundsgg/library-i18n")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
