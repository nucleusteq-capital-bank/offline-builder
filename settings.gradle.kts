rootProject.name = "opensky-backend"

pluginManagement {

    repositories {
        maven { url = uri("../offline-repo") }
    }

    resolutionStrategy {
        eachPlugin {

            if (requested.id.id == "org.springframework.boot") {
                useModule("org.springframework.boot:spring-boot-gradle-plugin:${requested.version}")
            }

            if (requested.id.id == "io.spring.dependency-management") {
                useModule("io.spring.gradle:dependency-management-plugin:${requested.version}")
            }

            if (requested.id.id == "org.sonarqube") {
                useModule("org.sonarsource.scanner.gradle:sonarqube-gradle-plugin:${requested.version}")
            }
        }
    }
}

dependencyResolutionManagement {
    repositories {
        maven { url = uri("../offline-repo") }
    }
}

include(
    "common",
    "partner",
    "admin",
    "product",
    "application"
)