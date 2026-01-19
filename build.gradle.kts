plugins {
    id("java")
}

group = "net.kcww.ec"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // --- Logging (SLF4J + Logback) ---
    // Logback Classic transitively pulls in slf4j-api, but defining both is explicit and safe.
    implementation("org.slf4j:slf4j-api:2.0.12")
    implementation("ch.qos.logback:logback-classic:1.5.3")

    // --- Lombok ---
    // Maven 'provided' translates to 'compileOnly' in Gradle.
    // We also need 'annotationProcessor' for Lombok to work.
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")

    // Optional: If you use Lombok annotations in your Test classes
    testCompileOnly("org.projectlombok:lombok:1.18.30")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.30")

    // --- Testing ---
    // JUnit 5 (Keep existing BOM structure)
    testImplementation(platform("org.junit:junit-bom:5.10.2")) // Updated to 5.10.2
    testImplementation("org.junit.jupiter:junit-jupiter")

    // Mockito (for JUnit 5)
    testImplementation("org.mockito:mockito-junit-jupiter:5.11.0")

    // AssertJ
    testImplementation("org.assertj:assertj-core:3.25.3")

    // Runtime engine for tests
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}