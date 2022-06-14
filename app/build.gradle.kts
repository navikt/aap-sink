plugins {
    id("com.github.johnrengelman.shadow")
    application
}

application {
    mainClass.set("app.AppKt")
}

dependencies {
    implementation("io.ktor:ktor-server-netty:2.0.1")
    implementation("io.ktor:ktor-server-metrics-micrometer:2.0.1")

    implementation("io.ktor:ktor-serialization-jackson:2.0.1")

    implementation("io.micrometer:micrometer-registry-prometheus:1.8.5")

    implementation("com.github.navikt.aap-libs:ktor-utils:0.0.43")
    implementation("com.github.navikt.aap-libs:kafka:2.0.1")

    runtimeOnly("ch.qos.logback:logback-classic:1.2.11")
    runtimeOnly("net.logstash.logback:logstash-logback-encoder:7.1.1")

    runtimeOnly("org.postgresql:postgresql:42.3.4")
    implementation("org.flywaydb:flyway-core:8.5.10")
    implementation("org.jetbrains.exposed:exposed-java-time:0.38.2")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.38.2")

    testImplementation(kotlin("test"))
    testImplementation("com.github.navikt.aap-libs:kafka-test:0.0.43")
    testImplementation("io.ktor:ktor-server-test-host:2.0.1")
    testImplementation("org.testcontainers:postgresql:1.17.1")
}
