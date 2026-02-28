plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.spring") version "2.2.21"
    `java-library`
    `maven-publish`
}

group = "dev.tokishu"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    // Use api() so JDA is transitively available to consumers
    api("net.dv8tion:JDA:6.3.1")
    implementation("org.reflections:reflections:0.10.2")

    // Spring Boot Integration (Optional)
    compileOnly("org.springframework.boot:spring-boot-autoconfigure:3.2.3")
    compileOnly("org.springframework:spring-context:6.1.4")
    
    // Configuration YAML (Jackson)
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
    
    // Dotenv
    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.8.0")
    
    // Logging Logging facade and default implementation
    api("org.slf4j:slf4j-api:2.0.12")
    implementation("ch.qos.logback:logback-classic:1.5.3")
    implementation("org.codehaus.janino:janino:3.1.12")
}

kotlin {
    jvmToolchain(21)
}

tasks.withType<Jar> {
}

tasks.register<JavaExec>("runApp") {
    mainClass.set("dev.tokishu.MainKt")
    classpath = sourceSets["main"].runtimeClasspath
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = "dev.tokishu"
            artifactId = "jdkit"
            version = "1.0.0"
        }
    }
}