plugins {
    kotlin("jvm") version "2.2.0"
}

group = "org.example.pamflet-dsl"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}