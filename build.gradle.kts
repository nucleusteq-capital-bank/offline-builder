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
    resolveAll("org.sonarqube:org.sonarqube.gradle.plugin:$sonarVersion@pom")

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

    // -------- Force common transitive roots --------
    resolveAll("com.fasterxml.jackson.core:jackson-databind")
    resolveAll("org.apache.commons:commons-compress")
    resolveAll("org.apache.httpcomponents.client5:httpclient5")
    resolveAll("org.antlr:antlr4-runtime")
    resolveAll("com.google.code.findbugs:jsr305")
}

// ---------------- Task ----------------

tasks.register("buildOfflineRepo") {

    doLast {

        println(" Resolving ALL components (including parents)...")

        val components = resolveAll
            .incoming
            .resolutionResult
            .allComponents

        components.forEach { comp ->

            val id = comp.id

            if (id is ModuleComponentIdentifier) {

                val groupPath = id.group.replace(".", "/")
                val targetDir = File(repoDir, "$groupPath/${id.module}/${id.version}")
                targetDir.mkdirs()

                println(" ${id.group}:${id.module}:${id.version}")

                // -------- Copy JARs --------
                val artifacts = resolveAll
                    .incoming
                    .artifacts
                    .artifacts
                    .filterIsInstance<ResolvedArtifactResult>()
                    .filter {
                        val cid = it.id.componentIdentifier as? ModuleComponentIdentifier
                        cid?.group == id.group &&
                        cid.module == id.module &&
                        cid.version == id.version
                    }

                artifacts.forEach {
                    it.file.copyTo(File(targetDir, it.file.name), overwrite = true)
                }

                // -------- Copy ALL POMs (including parents) --------
                val cacheDir = File(System.getProperty("user.home"))
                    .resolve(".gradle/caches/modules-2/files-2.1")
                    .resolve(id.group)
                    .resolve(id.module)
                    .resolve(id.version)

                if (cacheDir.exists()) {

                    cacheDir.walkTopDown()
                        .filter { it.isFile && it.name.endsWith(".pom") }
                        .forEach { pomFile ->

                            pomFile.copyTo(
                                File(targetDir, pomFile.name),
                                overwrite = true
                            )
                        }
                }
            }
        }

        println("\n Offline repo created at: ${repoDir.absolutePath}")
    }
}