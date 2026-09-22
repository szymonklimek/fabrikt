package com.cjbooms.fabrikt.generators.dependencies

data class DependencyNotation(
    val scope: Scope,
    val groupId: String,
    val artifactId: String,
    val version: String,
) {
    companion object {
        fun Scope.dependencyOf(gradleNotation: String) =
            gradleNotation
                .split(":")
                .let { (groupId, artifactId, version) ->
                    DependencyNotation(
                        scope = this,
                        groupId = groupId,
                        artifactId = artifactId,
                        version = version,
                    )
                }
    }

    /**
     * Scope as defined in: https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html#dependency-scope
     * Limited to used scopes in the project.
     */
    enum class Scope {
        COMPILE,
        RUNTIME,
    }
}
