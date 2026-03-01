plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.spring") version "2.2.21"
    id("com.vanniktech.maven.publish") version "0.36.0"
    id("org.jetbrains.dokka") version "2.1.0"
    `java-library`
    signing
}

group = "dev.tokishu"
version = "1.0.1-test"

repositories {
    mavenCentral()
}

dependencies {
    api("net.dv8tion:JDA:6.3.1")
    implementation("org.reflections:reflections:0.10.2")

    compileOnly("org.springframework.boot:spring-boot-autoconfigure:4.1.0-M2")
    compileOnly("org.springframework:spring-context:7.0.5")

    api("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    api("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.2")
    api("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")

    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.8.0")

    api("org.slf4j:slf4j-api:2.0.12")
    implementation("ch.qos.logback:logback-classic:1.5.32")
    implementation("org.codehaus.janino:janino:3.1.12")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.register<JavaExec>("runApp") {
    mainClass.set("dev.tokishu.MainKt")
    classpath = sourceSets["main"].runtimeClasspath
}

mavenPublishing {
    coordinates("dev.tokishu", "jdkit", project.version.toString())

    pom {
        name.set("JDKit")
        description.set("A lightweight, scalable, and Spring-integrated Discord bot framework built on top of JDA.")
        url.set("https://github.com/Tok1shu/JDKit")

        licenses {
            license {
                name.set("MIT License")
                url.set("https://mit-license.org/")
            }
        }
        developers {
            developer {
                id.set("P30wo9")
                name.set("Tokishu")
            }
        }
        scm {
            connection.set("scm:git:git://github.com/Tok1shu/JDKit.git")
            developerConnection.set("scm:git:ssh://github.com/Tok1shu/JDKit.git")
            url.set("https://github.com/Tok1shu/JDKit")
        }
    }

    publishToMavenCentral()
    signAllPublications()
}

signing {
    useGpgCmd()
}