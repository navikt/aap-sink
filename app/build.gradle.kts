plugins {
    id("com.github.johnrengelman.shadow")
    application
}

application {
    mainClass.set("app.AppKt")
}

dependencies {
    implementation("io.ktor:ktor-server-netty:2.0.2")
    implementation("io.ktor:ktor-server-metrics-micrometer:2.0.2")

    implementation("io.ktor:ktor-serialization-jackson:2.0.2")

    implementation("io.micrometer:micrometer-registry-prometheus:1.9.0")

    implementation("com.github.navikt.aap-libs:ktor-utils:2.1.3")
    implementation("com.github.navikt.aap-libs:kafka:2.0.6")

    runtimeOnly("ch.qos.logback:logback-classic:1.2.11")
    runtimeOnly("net.logstash.logback:logstash-logback-encoder:7.2")

    runtimeOnly("org.postgresql:postgresql:42.3.6")
    implementation("org.flywaydb:flyway-core:8.5.12")
    implementation("org.jetbrains.exposed:exposed-java-time:0.38.2")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.38.2")

    testImplementation(kotlin("test"))
    testImplementation("com.github.navikt.aap-libs:kafka-test:2.0.6")
    testImplementation("io.ktor:ktor-server-test-host:2.0.2")
    testImplementation("org.testcontainers:postgresql:1.17.2")
}
