
plugins {
    id ("org.jetbrains.kotlin.jvm") version "2.0.21"
    id ("io.github.rodm.teamcity-environments") version "1.5.5"
}

repositories {
    maven {
        url = uri(project.findProperty("teamcity.server.url") ?: "http://localhost:8111/app/dsl-plugins-repository")
        isAllowInsecureProtocol = true
    }
    mavenCentral()
}

dependencies {
    implementation ("org.jetbrains.teamcity:configs-dsl-kotlin-plugins-latest:1.0-SNAPSHOT")

    testImplementation ("org.junit.jupiter:junit-jupiter:5.12.2")
    testImplementation ("org.jetbrains.teamcity:server-api:2025.03")

    testRuntimeOnly ("org.junit.platform:junit-platform-launcher")
}

sourceSets {
    main {
        kotlin {
            srcDirs(".teamcity")
        }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

teamcity {
    environments {
        register("teamcity2022.10") {
            version = "2022.10.3"
        }
    }
}

tasks {
    test {
        useJUnitPlatform()
    }
}
