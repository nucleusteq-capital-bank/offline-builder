import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.attributes.Category
import java.io.File

// Versions
val springBootVersion = "3.5.6"
val dependencyManagementVersion = "1.1.6"
val sonarVersion = "5.0.0.4638"

val repoDir = file("offline-repo")

repositories {
    mavenCentral()
    gradlePluginPortal()
}

val resolveAll by configurations.creating {
    attributes {
        attribute(
            Category.CATEGORY_ATTRIBUTE,
            objects.named(Category.LIBRARY)
        )
    }
}

dependencies {

    // Plugin marker
    resolveAll("org.springframework.boot:org.springframework.boot.gradle.plugin:$springBootVersion")

    // Plugin implementations
    resolveAll("org.springframework.boot:spring-boot-gradle-plugin:$springBootVersion")
    resolveAll("io.spring.gradle:dependency-management-plugin:$dependencyManagementVersion")
    resolveAll("org.sonarsource.scanner.gradle:sonarqube-gradle-plugin:$sonarVersion")

    // Common Spring libraries
    resolveAll("org.springframework.boot:spring-boot-starter-web:$springBootVersion")
    resolveAll("org.springframework.boot:spring-boot-starter-data-jpa:$springBootVersion")
    resolveAll("org.springframework.boot:spring-boot-starter-test:$springBootVersion")
}

tasks.register("buildOfflineRepo") {

    doLast {

        val artifacts = resolveAll.incoming.artifacts.artifacts
            .filterIsInstance<ResolvedArtifactResult>()

        artifacts.forEach {

            val id = it.id.componentIdentifier as ModuleComponentIdentifier
            val group = id.group.replace(".", "/")

            val targetDir = File(repoDir, "$group/${id.module}/${id.version}")
            targetDir.mkdirs()

            it.file.copyTo(File(targetDir, it.file.name), overwrite = true)
        }

        println("Offline repo created at: ${repoDir.absolutePath}")
    }
}