import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
    java
    id("org.springframework.boot") version "3.3.5"
    id("io.spring.dependency-management") version "1.1.6"
}

group = "com.matador"
version = "0.1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

extra["springModulithVersion"] = "1.2.5"
extra["testcontainersVersion"] = "1.21.3"
extra["mapstructVersion"] = "1.6.3"

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-mail")

    // Spring Modulith
    implementation("org.springframework.modulith:spring-modulith-starter-core")
    implementation("org.springframework.modulith:spring-modulith-starter-jpa")
    testImplementation("org.springframework.modulith:spring-modulith-starter-test")

    // Persistence: Flyway, PostgreSQL, PostGIS
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.hibernate.orm:hibernate-spatial")
    runtimeOnly("org.postgresql:postgresql")

    // API docs
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")

    // Mapping
    implementation("org.mapstruct:mapstruct:${property("mapstructVersion")}")
    annotationProcessor("org.mapstruct:mapstruct-processor:${property("mapstructVersion")}")

    // JSON / JSONB support for JPA
    implementation("io.hypersistence:hypersistence-utils-hibernate-63:3.9.0")

    // UUIDv7
    implementation("com.fasterxml.uuid:java-uuid-generator:5.1.0")

    // JWT (RS256)
    implementation("com.nimbusds:nimbus-jose-jwt:9.41.2")

    // Distributed scheduler lock
    implementation("net.javacrumbs.shedlock:shedlock-spring:6.0.2")
    implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc-template:6.0.2")

    // Structured logging
    implementation("net.logstash.logback:logstash-logback-encoder:8.0")

    // Error tracking
    implementation("io.sentry:sentry-spring-boot-starter-jakarta:7.16.0")

    // External SDKs
    implementation("com.stripe:stripe-java:29.2.0")
    implementation("com.smartcar.sdk:java-sdk:4.7.3")
    implementation("com.twilio.sdk:twilio:10.9.2")
    implementation("com.postmarkapp:postmark:1.11.1")

    // R2 / S3-compatible object storage
    implementation("software.amazon.awssdk:s3:2.29.9")

    compileOnly("org.projectlombok:lombok:1.18.34")
    annotationProcessor("org.projectlombok:lombok:1.18.34")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.wiremock:wiremock-standalone:3.9.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.modulith:spring-modulith-bom:${property("springModulithVersion")}")
        mavenBom("org.testcontainers:testcontainers-bom:${property("testcontainersVersion")}")
    }
}

tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(
        listOf(
            "-parameters",
            "-Amapstruct.defaultComponentModel=spring",
            "-Amapstruct.unmappedTargetPolicy=ERROR",
        )
    )
}

tasks.withType<Test> {
    useJUnitPlatform()
    // docker-java defaults to API 1.32; modern Docker engines require >= 1.44.
    // Pin it for the forked test JVM (both the env var and docker-java's system property)
    // so Testcontainers works regardless of the host's Docker version.
    val dockerApiVersion = System.getenv("DOCKER_API_VERSION") ?: "1.44"
    environment("DOCKER_API_VERSION", dockerApiVersion)
    systemProperty("api.version", dockerApiVersion)
}

tasks.named<BootRun>("bootRun") {
    // Default to the dev profile when running locally.
    systemProperty("spring.profiles.active", System.getProperty("spring.profiles.active", "dev"))
}

// Writes the generated OpenAPI document to build/openapi/openapi.json.
// Requires the application to be running; springdoc serves it at /v3/api-docs.
tasks.register("generateOpenApiSpec") {
    group = "documentation"
    description = "Placeholder task; wired to springdoc-openapi-gradle-plugin in CI. See docs/BACKEND.md §10."
    doLast {
        logger.lifecycle("Run the app and GET /v3/api-docs to produce openapi.json (see CI workflow).")
    }
}
