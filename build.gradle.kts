import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import java.io.File

// ---------------- Versions ----------------

val springBootVersion = "3.5.6"
val dependencyManagementVersion = "1.1.6"
val sonarVersion = "5.0.0.4638"

// ---------------- Repo Output ----------------

val repoDir = file("offline-repo")

// ---------------- Repositories ----------------

repositories {
    mavenCentral()
    gradlePluginPortal()
}

// ---------------- Configuration ----------------

val resolveAll by configurations.creating {
    attributes {
        attribute(
            org.gradle.api.attributes.Category.CATEGORY_ATTRIBUTE,
            objects.named(org.gradle.api.attributes.Category.LIBRARY)
        )
    }
}

// ---------------- Dependencies to Resolve ----------------

dependencies {

    // ---------- Plugin Markers (REQUIRED for Gradle plugins) ----------

    resolveAll("org.springframework.boot:org.springframework.boot.gradle.plugin:$springBootVersion")
    resolveAll("io.spring.dependency-management:io.spring.dependency-management.gradle.plugin:$dependencyManagementVersion")

    // ---------- Plugin Implementations ----------

    resolveAll("org.springframework.boot:spring-boot-gradle-plugin:$springBootVersion")
    resolveAll("io.spring.gradle:dependency-management-plugin:$dependencyManagementVersion")
    resolveAll("org.sonarsource.scanner.gradle:sonarqube-gradle-plugin:$sonarVersion")

    // ---------- Core Spring Libraries ----------

    resolveAll("org.springframework.boot:spring-boot-starter-web:$springBootVersion")
    resolveAll("org.springframework.boot:spring-boot-starter-data-jpa:$springBootVersion")
    resolveAll("org.springframework.boot:spring-boot-starter-test:$springBootVersion")
}

// ---------------- Task to Build Offline Repo ----------------

tasks.register("buildOfflineRepo") {

    doLast {

        val artifacts = resolveAll.incoming.artifacts.artifacts
            .filterIsInstance<ResolvedArtifactResult>()

        artifacts.forEach {

            val id = it.id.componentIdentifier as ModuleComponentIdentifier
            val groupPath = id.group.replace(".", "/")
            val targetDir = File(repoDir, "$groupPath/${id.module}/${id.version}")
            targetDir.mkdirs()

            // copy jar
            it.file.copyTo(File(targetDir, it.file.name), overwrite = true)

            // download pom
            val pomUrl =
                "https://repo.maven.apache.org/maven2/$groupPath/${id.module}/${id.version}/${id.module}-${id.version}.pom"
            val pomFile = File(targetDir, "${id.module}-${id.version}.pom")
            try {
                pomUrl.toURL().openStream().use { input ->
                    pomFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (_: Exception) {
            }
        }
        println("Offline repository created at: ${repoDir.absolutePath}")
    }
}