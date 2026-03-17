rootProject.name = "offline-builder"

pluginManagement {

    repositories {

        maven {
            url = uri("C:/Users/NT-RNagaraju/code/azureRepo/offline-repo")

            metadataSources {
                mavenPom()
                artifact()
            }
        }

        // remove internet repos for true offline
    }
}

dependencyResolutionManagement {

    repositories {

        maven {
            url = uri("C:/Users/NT-RNagaraju/code/azureRepo/offline-repo")

            metadataSources {
                mavenPom()
                artifact()
            }
        }
    }
}

rootProject.name = "opensky-backend"