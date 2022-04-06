import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow")
    application
}

dependencies {
    implementation("io.ktor:ktor-server-netty:1.6.7")

    implementation("io.ktor:ktor-metrics-micrometer:1.6.7")
    implementation("io.micrometer:micrometer-registry-prometheus:1.8.3")

    implementation("com.sksamuel.hoplite:hoplite-yaml:2.1.0")

    implementation("org.apache.kafka:kafka-clients:3.1.0")
    implementation("org.apache.kafka:kafka-streams:3.1.0")
    implementation("io.confluent:kafka-streams-avro-serde:7.0.1") {
        exclude("org.apache.kafka", "kafka-clients")
    }

    runtimeOnly("ch.qos.logback:logback-classic:1.2.10")
    runtimeOnly("net.logstash.logback:logstash-logback-encoder:7.0.1")

    implementation("no.nav.aap.avro:vedtak:1.1.10")

    runtimeOnly("org.postgresql:postgresql:42.3.3")
    implementation("org.flywaydb:flyway-core:8.5.1")
    implementation("org.jetbrains.exposed:exposed-java-time:0.37.3")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.37.3")

    testRuntimeOnly("com.h2database:h2:2.1.210")
    testImplementation(kotlin("test"))
    testImplementation("org.apache.kafka:kafka-streams-test-utils:3.1.0")
    testImplementation("io.ktor:ktor-server-test-host:1.6.7")
    testImplementation("uk.org.webcompere:system-stubs-jupiter:2.0.1")
}

application {
    mainClass.set("app.AppKt")
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    withType<Test> {
        useJUnitPlatform()
    }
}

kotlin.sourceSets["main"].kotlin.srcDirs("main")
kotlin.sourceSets["test"].kotlin.srcDirs("test")
sourceSets["main"].resources.srcDir("main")