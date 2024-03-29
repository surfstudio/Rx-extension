package ru.surfstudio.android.build.artifactory

import org.gradle.api.GradleException
import ru.surfstudio.android.build.Components
import ru.surfstudio.android.build.exceptions.ArtifactNotExistInArtifactoryException
import ru.surfstudio.android.build.exceptions.FolderNotFoundException
import ru.surfstudio.android.build.exceptions.LibraryNotFoundException
import ru.surfstudio.android.build.model.ArtifactInfo
import ru.surfstudio.android.build.model.Component
import ru.surfstudio.android.build.model.module.Library

/**
 * Provide Artifactory functions
 */
object Artifactory {

    private val repository = ArtifactoryRepository()

    /**
     * Deploy artifact to bintray
     */
    fun distributeArtifactToBintray(overrideExisting: Boolean, libraryNames: List<String>) {
        val artifacts: List<ArtifactInfo> = libraryNames.map { ArtifactInfo(it) }

        var packagesRepoPaths = ""
        artifacts.forEachIndexed { index, artifactInfo ->
            packagesRepoPaths += artifactInfo.getPath()
            if (index != artifacts.size - 1) packagesRepoPaths += ", "
        }

        val response = repository.distribute(packagesRepoPaths, overrideExisting)

        if (response.statusCode != 200) throw GradleException(response.toString())
    }

    /**
     * Check libraries's android standard dependencies exist in artifactory
     */
    fun checkLibrariesStandardDependenciesExisting(component: Component) {
        component.libraries.forEach { checkLibraryStandardDependenciesExisting(it, component) }
    }

    /**
     * Check is library deployed
     */
    @JvmStatic
    fun isLibraryAlreadyDeployed(libraryName: String): Boolean {
        val library = Components.libraries.find { it.name == libraryName }
                ?: throw LibraryNotFoundException(libraryName)
        return isArtifactExists(library.name, library.projectVersion)
    }

    /**
     * Check library android standard dependencies exist in artifactory
     */
    private fun checkLibraryStandardDependenciesExisting(library: Library, component: Component) {
        library.androidStandardDependencies
                .filter { it.component != component }
                .forEach { androidStandardDependency ->
            if (!isArtifactExists(androidStandardDependency.name, androidStandardDependency.component.projectVersion)) {
                throw ArtifactNotExistInArtifactoryException(library.name, androidStandardDependency)
            }
            Components.libraries.find { it.name == androidStandardDependency.name }?.let {
                checkLibraryStandardDependenciesExisting(it, component)
            }
        }
    }

    /**
     * Check artifact exist in artifactory
     */
    fun isArtifactExists(artifactName: String, version: String): Boolean {
        val folderPath = "$artifactName/$version"
        return try {
            !repository.getFolderInfo(folderPath).isEmpty
        } catch (e: FolderNotFoundException) {
            false
        }
    }
}