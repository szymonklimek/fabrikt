package com.cjbooms.fabrikt.generators.dependencies

import com.cjbooms.fabrikt.cli.ClientCodeGenOptionType
import com.cjbooms.fabrikt.cli.ClientCodeGenTargetType
import com.cjbooms.fabrikt.cli.CodeGenTypeOverride
import com.cjbooms.fabrikt.cli.CodeGenerationType
import com.cjbooms.fabrikt.cli.ControllerCodeGenOptionType
import com.cjbooms.fabrikt.cli.ControllerCodeGenTargetType
import com.cjbooms.fabrikt.cli.InstantLibrary
import com.cjbooms.fabrikt.cli.ModelCodeGenOptionType
import com.cjbooms.fabrikt.cli.SerializationLibrary
import com.cjbooms.fabrikt.cli.ValidationLibrary
import com.cjbooms.fabrikt.generators.MutableSettings
import com.cjbooms.fabrikt.util.ModelNameRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DependenciesResolverTest {
    @BeforeEach
    fun init() {
        MutableSettings.updateSettings()
        ModelNameRegistry.clear()
    }

    @Test
    fun `defaults always include validation and jackson without client or controller artifacts`() {
        val coordinates = deps().coordinates()

        assertThat(coordinates).contains(
            "jakarta.validation:jakarta.validation-api",
            "com.fasterxml.jackson.core:jackson-core",
            "com.fasterxml.jackson.core:jackson-databind",
            "com.fasterxml.jackson.module:jackson-module-kotlin",
        )
        assertThat(coordinates).doesNotContain(
            "com.squareup.okhttp3:okhttp",
            "org.springframework:spring-webmvc",
            "io.github.openfeign:feign-core",
        )
    }

    @Test
    fun `client resolution uses client target not controller target`() {
        val coordinates =
            deps(
                generatorTypes = setOf(CodeGenerationType.CLIENT),
                clientTarget = ClientCodeGenTargetType.OK_HTTP,
                controllerTarget = ControllerCodeGenTargetType.SPRING,
            ).coordinates()

        assertThat(coordinates).contains("com.squareup.okhttp3:okhttp")
        assertThat(coordinates).doesNotContain("org.springframework:spring-webmvc")
    }

    @Test
    fun `client options contribute dependencies`() {
        val coordinates =
            deps(
                generatorTypes = setOf(CodeGenerationType.CLIENT),
                clientTarget = ClientCodeGenTargetType.OK_HTTP,
                clientOptions = setOf(ClientCodeGenOptionType.RESILIENCE4J),
            ).coordinates()

        assertThat(coordinates).contains("io.github.resilience4j:resilience4j-circuitbreaker")
    }

    @Test
    fun `open feign client target contributes feign not okhttp`() {
        val coordinates =
            deps(
                generatorTypes = setOf(CodeGenerationType.CLIENT),
                clientTarget = ClientCodeGenTargetType.OPEN_FEIGN,
            ).coordinates()

        assertThat(coordinates).contains("io.github.openfeign:feign-core")
        assertThat(coordinates).doesNotContain("com.squareup.okhttp3:okhttp")
    }

    @Test
    fun `controllers gated on CONTROLLERS generation type`() {
        val coordinates =
            deps(
                generatorTypes = setOf(CodeGenerationType.CONTROLLERS),
                controllerTarget = ControllerCodeGenTargetType.MICRONAUT,
            ).coordinates()

        assertThat(coordinates).contains(
            "io.micronaut:micronaut-http",
            "io.micronaut.security:micronaut-security",
        )
    }

    @Test
    fun `client only does not include controller target dependencies`() {
        val coordinates =
            deps(
                generatorTypes = setOf(CodeGenerationType.CLIENT),
                clientTarget = ClientCodeGenTargetType.OK_HTTP,
                controllerTarget = ControllerCodeGenTargetType.MICRONAUT,
            ).coordinates()

        assertThat(coordinates).doesNotContain(
            "io.micronaut:micronaut-http",
            "io.micronaut.security:micronaut-security",
        )
    }

    @Test
    fun `controller options contribute dependencies`() {
        val coordinates =
            deps(
                generatorTypes = setOf(CodeGenerationType.CONTROLLERS),
                controllerOptions = setOf(ControllerCodeGenOptionType.SUSPEND_MODIFIER),
            ).coordinates()

        assertThat(coordinates).contains("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    }

    @Test
    fun `model options contribute dependencies`() {
        val coordinates =
            deps(
                modelOptions = setOf(ModelCodeGenOptionType.QUARKUS_REFLECTION),
            ).coordinates()

        assertThat(coordinates).contains("io.quarkus:quarkus-core")
    }

    @Test
    fun `serialization library swap changes model dependencies`() {
        val coordinates =
            deps(
                serializationLibrary = SerializationLibrary.KOTLINX_SERIALIZATION,
            ).coordinates()

        assertThat(coordinates).contains("org.jetbrains.kotlinx:kotlinx-serialization-json")
        assertThat(coordinates).doesNotContain("com.fasterxml.jackson.core:jackson-databind")
    }

    @Test
    fun `no validation omits validation api`() {
        val coordinates =
            deps(
                validationLibrary = ValidationLibrary.NO_VALIDATION,
            ).coordinates()

        assertThat(coordinates).doesNotContain(
            "jakarta.validation:jakarta.validation-api",
            "javax.validation:validation-api",
        )
    }

    @Test
    fun `instant library included when DATETIME_AS_INSTANT override is set`() {
        val coordinates =
            deps(
                typeOverrides = setOf(CodeGenTypeOverride.DATETIME_AS_INSTANT),
                instantLibrary = InstantLibrary.KOTLINX_INSTANT,
            ).coordinates()

        assertThat(coordinates).contains("org.jetbrains.kotlinx:kotlinx-datetime")
    }

    @Test
    fun `instant library omitted without DATETIME_AS_INSTANT override`() {
        val coordinates =
            deps(
                instantLibrary = InstantLibrary.KOTLINX_INSTANT,
            ).coordinates()

        assertThat(coordinates).doesNotContain("org.jetbrains.kotlinx:kotlinx-datetime")
    }

    @Test
    fun `duplicate coordinates from overlapping model options are deduplicated`() {
        val list =
            deps(
                modelOptions =
                    setOf(
                        ModelCodeGenOptionType.MICRONAUT_INTROSPECTION,
                        ModelCodeGenOptionType.MICRONAUT_REFLECTION,
                    ),
            )

        assertThat(list.filter { it.groupId == "io.micronaut" && it.artifactId == "micronaut-core" })
            .hasSize(1)
    }

    private fun deps(
        generatorTypes: Set<CodeGenerationType> = emptySet(),
        controllerOptions: Set<ControllerCodeGenOptionType> = emptySet(),
        controllerTarget: ControllerCodeGenTargetType = ControllerCodeGenTargetType.default,
        modelOptions: Set<ModelCodeGenOptionType> = emptySet(),
        clientOptions: Set<ClientCodeGenOptionType> = emptySet(),
        clientTarget: ClientCodeGenTargetType = ClientCodeGenTargetType.default,
        typeOverrides: Set<CodeGenTypeOverride> = emptySet(),
        validationLibrary: ValidationLibrary = ValidationLibrary.default,
        serializationLibrary: SerializationLibrary = SerializationLibrary.default,
        instantLibrary: InstantLibrary = InstantLibrary.default,
    ): List<DependencyNotation> =
        DependenciesResolver(
            generatorTypes = generatorTypes,
            controllerOptions = controllerOptions,
            controllerTarget = controllerTarget,
            modelOptions = modelOptions,
            clientOptions = clientOptions,
            clientTarget = clientTarget,
            typeOverrides = typeOverrides,
            validationLibrary = validationLibrary,
            serializationLibrary = serializationLibrary,
            instantLibrary = instantLibrary,
        ).resolve()

    private fun List<DependencyNotation>.coordinates(): List<String> = map { "${it.groupId}:${it.artifactId}" }
}
