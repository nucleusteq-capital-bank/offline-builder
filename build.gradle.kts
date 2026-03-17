import java.io.File

// ---------------- Versions ----------------

val springBootVersion = "3.5.6"
val sonarVersion = "5.0.0.4638"
val jacksonVersion = "2.19.2"
val commonsCompressVersion = "1.27.1"
val httpClientVersion = "5.5"
val sonarScannerApiVersion = "2.16.2.588"

// Parent POM versions
val commonsParentVersion = "72"
val jacksonBaseVersion = "2.19.2"
val httpClientParentVersion = "5.5"
val sonarScannerParentVersion = "2.16.2.588"
val ossParentVersion = "7"

// ---------------- Repo ----------------

val repoDir = file("offline-repo")

repositories {
    mavenCentral()
    gradlePluginPortal()
}

// ---------------- Configuration ----------------

val resolveAll by configurations.creating {
    isCanBeResolved = true
    isTransitive = true
}

// ---------------- Dependencies ----------------

dependencies {

    // -------- Plugins --------
    resolveAll("org.springframework.boot:spring-boot-gradle-plugin:$springBootVersion")
    resolveAll("org.sonarsource.scanner.gradle:sonarqube-gradle-plugin:$sonarVersion")

    // -------- Plugin markers --------
    resolveAll("org.springframework.boot:org.springframework.boot.gradle.plugin:$springBootVersion@pom")
    resolveAll("org.sonarqube:org.sonarqube.gradle.plugin:$sonarVersion@pom")

    // -------- Spring --------
    resolveAll("org.springframework.boot:spring-boot-starter-web:$springBootVersion")

    // -------- Parent POMs (CRITICAL) --------
    resolveAll("org.apache.commons:commons-parent:$commonsParentVersion@pom")
    resolveAll("com.fasterxml.jackson:jackson-base:$jacksonBaseVersion@pom")
    resolveAll("org.apache.httpcomponents.client5:httpclient5-parent:$httpClientParentVersion@pom")
    resolveAll("org.sonarsource.scanner.api:sonar-scanner-api-parent:$sonarScannerParentVersion@pom")
    resolveAll("org.sonatype.oss:oss-parent:$ossParentVersion@pom")

    // -------- Core dependencies --------
    resolveAll("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    resolveAll("org.apache.commons:commons-compress:$commonsCompressVersion")
    resolveAll("org.apache.httpcomponents.client5:httpclient5:$httpClientVersion")
}

// ---------------- Task ----------------

tasks.register("buildOfflineRepo") {

    doLast {

        println("Building FULL offline repo...")

        resolveAll.resolve()

        val cacheRoot = File(System.getProperty("user.home"))
            .resolve(".gradle/caches/modules-2/files-2.1")

        cacheRoot.walkTopDown().forEach { file ->

            if (file.isFile && (file.name.endsWith(".jar") || file.name.endsWith(".pom"))) {

                val relative = file.absolutePath.substringAfter("files-2.1\\")
                val parts = relative.split(File.separator)

                if (parts.size >= 4) {

                    val group = parts[0]
                    val module = parts[1]
                    val version = parts[2]

                    val targetDir = repoDir
                        .resolve(group.replace(".", "/"))
                        .resolve(module)
                        .resolve(version)

                    targetDir.mkdirs()

                    file.copyTo(
                        targetDir.resolve(file.name),
                        overwrite = true
                    )
                }
            }
        }

        println("Offline repo ready at: ${repoDir.absolutePath}")
    }
}