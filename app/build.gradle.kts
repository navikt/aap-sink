plugins {
    id("io.ktor.plugin")
}

application {
    mainClass.set("app.AppKt")
}

val aapLibVersion = "3.7.16"
val ktorVersion = "2.2.4"

dependencies {
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")

    implementation("com.github.navikt.aap-libs:ktor-utils:$aapLibVersion")
    implementation("com.github.navikt.aap-libs:kafka-2:$aapLibVersion")

    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.14.2")
    implementation("io.micrometer:micrometer-registry-prometheus:1.10.6")
    runtimeOnly("ch.qos.logback:logback-classic:1.4.6")
    implementation("net.logstash.logback:logstash-logback-encoder:7.3")
    runtimeOnly("org.postgresql:postgresql:42.5.4")
    implementation("org.flywaydb:flyway-core:9.16.0")
    implementation("org.jetbrains.exposed:exposed-java-time:0.41.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.41.1")

    testImplementation("io.ktor:ktor-client-cio:$ktorVersion")
    testImplementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    testImplementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")

    testImplementation("com.github.navikt.aap-libs:kafka-test-2:$aapLibVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("org.testcontainers:postgresql:1.17.6")
    testImplementation(kotlin("test"))
}
