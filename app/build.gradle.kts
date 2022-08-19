plugins {
    id("io.ktor.plugin")
    application
}

application {
    mainClass.set("app.AppKt")
}

val aapLibVersion = "3.1.11"
val ktorVersion = "2.1.0"

dependencies {
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")

    implementation("com.github.navikt.aap-libs:ktor-utils:$aapLibVersion")
    implementation("com.github.navikt.aap-libs:kafka:$aapLibVersion")

    implementation("io.micrometer:micrometer-registry-prometheus:1.9.2")
    runtimeOnly("ch.qos.logback:logback-classic:1.2.11")
    implementation("net.logstash.logback:logstash-logback-encoder:7.2")
    runtimeOnly("org.postgresql:postgresql:42.4.2")
    implementation("org.flywaydb:flyway-core:9.0.4")
    implementation("org.jetbrains.exposed:exposed-java-time:0.38.2")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.38.2")

    testImplementation("com.github.navikt.aap-libs:kafka-test:$aapLibVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("org.testcontainers:postgresql:1.17.3")
    testImplementation(kotlin("test"))
}
