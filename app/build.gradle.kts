plugins {
    id("io.ktor.plugin")
    application
}

application {
    mainClass.set("app.AppKt")
}

val aapLibVersion = "3.1.11"
val ktorVersion = "2.1.2"

dependencies {
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")

    implementation("com.github.navikt.aap-libs:ktor-utils:$aapLibVersion")
    implementation("com.github.navikt.aap-libs:kafka:$aapLibVersion")

    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.13.3")
    implementation("io.micrometer:micrometer-registry-prometheus:1.9.3")
    runtimeOnly("ch.qos.logback:logback-classic:1.4.0")
    implementation("net.logstash.logback:logstash-logback-encoder:7.2")
    runtimeOnly("org.postgresql:postgresql:42.5.0")
    implementation("org.flywaydb:flyway-core:9.2.1")
    implementation("org.jetbrains.exposed:exposed-java-time:0.39.2")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.39.2")

    testImplementation("io.ktor:ktor-client-cio:$ktorVersion")
    testImplementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    testImplementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")

    testImplementation("com.github.navikt.aap-libs:kafka-test:$aapLibVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("org.testcontainers:postgresql:1.17.3")
    testImplementation(kotlin("test"))
}
