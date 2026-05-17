plugins {
    `java`
    `maven-publish`
}

group = "com.midknightgarden"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly("net.kyori:adventure-api:4.14.0")
    compileOnly("net.kyori:adventure-platform-bukkit:4.1.0")
    implementation("org.xerial:sqlite-jdbc:3.42.0.0")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}
