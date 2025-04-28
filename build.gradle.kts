
plugins {
    id ("org.gradle.jacoco")
    alias (libs.plugins.kotlin.jvm)
    alias (libs.plugins.sonarqube)
    alias (libs.plugins.teamcity.environments)
}

group = "com.github.rodm"
version = "1.0-SNAPSHOT"

repositories {
    maven {
        url = uri(project.findProperty("teamcity.server.url") ?: "http://localhost:8111/app/dsl-plugins-repository")
        isAllowInsecureProtocol = true
    }
    mavenCentral()
}

dependencies {
    implementation (libs.teamcity.configs.dsl.plugins)

    testImplementation (libs.junit.jupiter)
    testImplementation (libs.teamcity.server.api)

    testRuntimeOnly (libs.junit.launcher)
}

sourceSets {
    main {
        kotlin {
            srcDirs("settings")
        }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

sonarqube {
    properties {
        property ("sonar.kotlin.file.suffixes", "kt,kts")
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
        finalizedBy (jacocoTestReport)
    }

    jacocoTestReport {
        reports {
            xml.required.set(true)
        }
    }
}
