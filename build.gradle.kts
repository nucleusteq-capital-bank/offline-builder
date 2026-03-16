import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import java.io.File

val springBootVersion = "3.5.6"
val dependencyManagementVersion = "1.1.7"
val sonarVersion = "5.0.0.4638"

val repoDir = file("offline-repo")

repositories {
    mavenCentral()
    gradlePluginPortal()
}

val resolveAll by configurations.creating {
    attributes {
        attribute(
            org.gradle.api.attributes.Bundling.BUNDLING_ATTRIBUTE,
            objects.named(org.gradle.api.attributes.Bundling.EXTERNAL)
        )
    }
}

dependencies {

    // Plugin markers
    resolveAll("org.springframework.boot:org.springframework.boot.gradle.plugin:$springBootVersion")
    resolveAll("io.spring.dependency-management:io.spring.dependency-management.gradle.plugin:$dependencyManagementVersion")

    // Plugin implementations
    resolveAll("org.springframework.boot:spring-boot-gradle-plugin:$springBootVersion")
    resolveAll("io.spring.gradle:dependency-management-plugin:$dependencyManagementVersion")
    resolveAll("org.sonarsource.scanner.gradle:sonarqube-gradle-plugin:$sonarVersion")

    // Common Spring dependencies
    resolveAll("org.springframework.boot:spring-boot-starter-web:$springBootVersion")
    resolveAll("org.springframework.boot:spring-boot-starter-data-jpa:$springBootVersion")
    resolveAll("org.springframework.boot:spring-boot-starter-test:$springBootVersion")
}

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

            // Locate POM in Gradle cache
            val cacheDir = File(System.getProperty("user.home"))
                .resolve(".gradle/caches/modules-2/files-2.1")
                .resolve(id.group)
                .resolve(id.module)
                .resolve(id.version)

            val pom = cacheDir
                .walkTopDown()
                .firstOrNull { f -> f.name.endsWith(".pom") }

            if (pom != null) {
                pom.copyTo(File(targetDir, pom.name), overwrite = true)
            }
        }

        println("Offline repo created at: ${repoDir.absolutePath}")
    }
}