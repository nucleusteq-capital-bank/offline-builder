import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import java.io.File

// ---------------- Versions ----------------

val springBootVersion = "3.5.6"
val dependencyManagementVersion = "1.1.7"
val sonarVersion = "5.0.0.4638"

// ---------------- Output repo ----------------

val repoDir = file("offline-repo")

repositories {
    mavenCentral()
    gradlePluginPortal()
}

// ---------------- Configuration ----------------

val resolveAll by configurations.creating {

    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = true

    attributes {

        attribute(
            org.gradle.api.attributes.Category.CATEGORY_ATTRIBUTE,
            objects.named(org.gradle.api.attributes.Category.LIBRARY)
        )

        attribute(
            org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE,
            objects.named(org.gradle.api.attributes.Usage.JAVA_RUNTIME)
        )
    }
}

// ---------------- Dependencies ----------------

dependencies {

    // -------- Plugin markers (POM ONLY) --------

    resolveAll("org.springframework.boot:org.springframework.boot.gradle.plugin:$springBootVersion@pom")
    resolveAll("io.spring.dependency-management:io.spring.dependency-management.gradle.plugin:$dependencyManagementVersion@pom")

    // -------- Plugin implementations --------

    resolveAll("org.springframework.boot:spring-boot-gradle-plugin:$springBootVersion")
    resolveAll("io.spring.gradle:dependency-management-plugin:$dependencyManagementVersion")
    resolveAll("org.sonarsource.scanner.gradle:sonarqube-gradle-plugin:$sonarVersion")

    // -------- Spring Boot BOM --------

    resolveAll("org.springframework.boot:spring-boot-dependencies:$springBootVersion@pom")

    // -------- Core starters --------

    resolveAll("org.springframework.boot:spring-boot-starter:$springBootVersion")
    resolveAll("org.springframework.boot:spring-boot-starter-web:$springBootVersion")
    resolveAll("org.springframework.boot:spring-boot-starter-data-jpa:$springBootVersion")
    resolveAll("org.springframework.boot:spring-boot-starter-test:$springBootVersion")

    // -------- Common transitive roots --------

    resolveAll("com.fasterxml.jackson.core:jackson-databind")
    resolveAll("org.apache.commons:commons-compress")
    resolveAll("org.apache.httpcomponents.client5:httpclient5")
    resolveAll("org.antlr:antlr4-runtime")
    resolveAll("com.google.code.findbugs:jsr305")
}

// ---------------- Task to build repo ----------------

tasks.register("buildOfflineRepo") {

    doLast {

        val artifacts = resolveAll
            .incoming
            .artifacts
            .artifacts
            .filterIsInstance<ResolvedArtifactResult>()

        artifacts.forEach {

            val id = it.id.componentIdentifier as ModuleComponentIdentifier
            val groupPath = id.group.replace(".", "/")

            val targetDir = File(repoDir, "$groupPath/${id.module}/${id.version}")
            targetDir.mkdirs()

            // Copy JAR
            it.file.copyTo(File(targetDir, it.file.name), overwrite = true)

            // Copy POM from Gradle cache
            val cacheDir = File(System.getProperty("user.home"))
                .resolve(".gradle/caches/modules-2/files-2.1")
                .resolve(id.group)
                .resolve(id.module)
                .resolve(id.version)

            val pom = cacheDir.walkTopDown()
                .firstOrNull { f -> f.name.endsWith(".pom") }

            if (pom != null) {
                pom.copyTo(File(targetDir, pom.name), overwrite = true)
            }
        }

        println("Offline repo created at: ${repoDir.absolutePath}")
    }
}