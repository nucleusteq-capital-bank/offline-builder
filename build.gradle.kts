import java.io.File
import java.net.URL

plugins {
    id("base")
}

val repoDir = File(rootDir, "offline-repo")

repositories {
    mavenCentral()
    gradlePluginPortal()
}


val deps = listOf(

    //  Spring Boot plugin marker (REQUIRED)
    "org.springframework.boot:org.springframework.boot.gradle.plugin:3.5.6",

    //  Spring Boot actual plugin
    "org.springframework.boot:spring-boot-gradle-plugin:3.5.6",

    //  Runtime
    "org.springframework.boot:spring-boot-starter-web:3.5.6",

    //  Sonar actual plugin (NO marker!)
    "org.sonarsource.scanner.gradle:sonarqube-gradle-plugin:5.0.0.4638"
)

val resolveAll = configurations.create("resolveAll")

dependencies {
    deps.forEach { add("resolveAll", it) }
}

// ------------------------------------------------------------
//  TASK: BUILD OFFLINE REPO
// ------------------------------------------------------------
tasks.register("buildOfflineRepo") {

    doLast {

        println(" Building offline repo at: ${repoDir.absolutePath}")

        repoDir.mkdirs()

        val baseUrl = "https://repo.maven.apache.org/maven2"

        resolveAll.resolvedConfiguration.resolvedArtifacts.forEach { artifact ->

            val group = artifact.moduleVersion.id.group
            val name = artifact.name
            val version = artifact.moduleVersion.id.version

            val groupPath = group.replace(".", "/")

            val targetDir = repoDir
                .resolve(groupPath)
                .resolve(name)
                .resolve(version)

            targetDir.mkdirs()

            // -----------------------------
            // Download JAR
            // -----------------------------
            val jarFile = targetDir.resolve("$name-$version.jar")
            val jarUrl = "$baseUrl/$groupPath/$name/$version/$name-$version.jar"

            if (!jarFile.exists()) {
                runCatching {
                    jarFile.writeBytes(URL(jarUrl).readBytes())
                    println("✔ JAR: $group:$name:$version")
                }
            }

            // -----------------------------
            // Download POM + parents
            // -----------------------------
            downloadPomRecursive(group, name, version, repoDir)
        }

        println(" Offline repo ready!")
    }
}

// ------------------------------------------------------------
//  RECURSIVE POM DOWNLOAD
// ------------------------------------------------------------
fun downloadPomRecursive(group: String, name: String, version: String, repoDir: File) {

    val baseUrl = "https://repo.maven.apache.org/maven2"
    val groupPath = group.replace(".", "/")

    val targetDir = repoDir
        .resolve(groupPath)
        .resolve(name)
        .resolve(version)

    targetDir.mkdirs()

    val pomFile = targetDir.resolve("$name-$version.pom")

    if (!pomFile.exists()) {

        try {
            val pomUrl = "$baseUrl/$groupPath/$name/$version/$name-$version.pom"
            val pomText = URL(pomUrl).readText()

            pomFile.writeText(pomText)
            println("✔ POM: $group:$name:$version")

            // -----------------------------
            // FIND PARENT
            // -----------------------------
            val parentRegex = Regex("<parent>(.*?)</parent>", RegexOption.DOT_MATCHES_ALL)
            val groupRegex = Regex("<groupId>(.*?)</groupId>")
            val artifactRegex = Regex("<artifactId>(.*?)</artifactId>")
            val versionRegex = Regex("<version>(.*?)</version>")

            val parentBlock = parentRegex.find(pomText)?.value

            if (parentBlock != null) {

                val parentGroup = groupRegex.find(parentBlock)?.groupValues?.get(1)
                val parentArtifact = artifactRegex.find(parentBlock)?.groupValues?.get(1)
                val parentVersion = versionRegex.find(parentBlock)?.groupValues?.get(1)

                if (parentGroup != null && parentArtifact != null && parentVersion != null) {

                    //  recursive
                    downloadPomRecursive(parentGroup, parentArtifact, parentVersion, repoDir)
                }
            }

        } catch (e: Exception) {
            println("⚠ Failed POM: $group:$name:$version")
        }
    }
}