package org.shrader.repomirror

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.artifacts.result.UnresolvedDependencyResult
import org.gradle.api.component.Artifact
import org.gradle.api.component.Component
import org.gradle.api.tasks.TaskAction
import org.gradle.jvm.JvmLibrary
import org.gradle.language.base.artifact.SourcesArtifact
import org.gradle.language.java.artifact.JavadocArtifact
import org.gradle.maven.MavenModule
import org.gradle.maven.MavenPomArtifact
import java.io.File
import kotlin.reflect.KClass

/**
 * Created by jshrader on 6/9/16.
 */
open class RepoMirror : DefaultTask() {
    @TaskAction
    fun mirror() {
        val componentIds = project.configurations.flatMap { config ->
            config.incoming.resolutionResult.allDependencies.mapNotNull { dep ->
                if (dep is ResolvedDependencyResult) {
                    dep.selected.id
                } else {
                    if (dep is UnresolvedDependencyResult) {
                        println("${dep.requested} is not resolved: ${dep.failure}")
                    }
                    null
                }
            }
        }

        val files = setOf(
                configurationFiles(),
                artifacts(componentIds, JvmLibrary::class, SourcesArtifact::class),
                artifacts(componentIds, JvmLibrary::class, JavadocArtifact::class),
                artifacts(componentIds, MavenModule::class, MavenPomArtifact::class)
        ).flatten()

        files.sorted().forEach { println(it) }
    }

    fun configurationFiles(): Collection<File> = project.configurations.flatMap { it -> it.files }

    fun artifacts(componentIds: List<ComponentIdentifier>,
                  componentType: KClass<out Component>,
                  artifactType: KClass<out Artifact>): Collection<File> {
        return project.dependencies.createArtifactResolutionQuery()
                .forComponents(componentIds)
                .withArtifacts(componentType.java, artifactType.java)
                .execute().let { queryResult ->
            queryResult.resolvedComponents.flatMap { comp ->
                comp.getArtifacts(artifactType.java).filterIsInstance<ResolvedArtifactResult>().map { artifact ->
                    artifact.file
                }
            }
        }
    }
}