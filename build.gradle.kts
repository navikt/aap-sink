import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.10"
    id("io.ktor.plugin") version "2.1.1" apply false
}

allprojects {
    repositories {
        mavenCentral()
        maven("https://jitpack.io")
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    tasks {
        withType<KotlinCompile> {
            kotlinOptions.jvmTarget = "18"
        }

        withType<Test> {
            useJUnitPlatform()
        }
    }

    kotlin.sourceSets["main"].kotlin.srcDirs("main")
    kotlin.sourceSets["test"].kotlin.srcDirs("test")
    sourceSets["main"].resources.srcDir("main")
    sourceSets["test"].resources.srcDir("test")
}