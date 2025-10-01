plugins {
    kotlin("jvm") version "2.2.0"
    id("com.vanniktech.maven.publish") version "0.34.0"
}

val appName = "pamflet-dsl"
val repoName = "pamflet-dsl-kotlin"

group = "org.example.$appName"
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

mavenPublishing {
    publishToMavenCentral()

    signAllPublications()

    coordinates(group.toString(), appName, version.toString())

    pom {
        name = "Pamflet DSL"
        description = "A language parser for $appName; the markup language for the pamflet flashcard app (android port)"
        inceptionYear = "2025"
        url = "https://github.com/craftzniac/$repoName"
        licenses {
            license {
                name = "GNU GENERAL PUBLIC LICENSE, Version 3"
                url = "https://www.gnu.org/licenses/gpl-3.0.txt"
                distribution = "https://www.gnu.org/licenses/gpl-3.0.txt"
            }
        }
        developers {
            developer {
                id = "craftzniac"
                name = "Craftzniac"
                url = "https://github.com/craftzniac/"
            }
        }
        scm {
            url = "https://github.com/craftzniac/$repoName/"
            connection = "scm:git:git://github.com/craftzniac/$repoName.git"
            developerConnection = "scm:git:ssh://git@github.com/craftzniac/$repoName.git"
        }
    }
}