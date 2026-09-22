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

class DependenciesResolver(
    private val generatorTypes: Set<CodeGenerationType>,
    private val controllerOptions: Set<ControllerCodeGenOptionType>,
    private val controllerTarget: ControllerCodeGenTargetType,
    private val modelOptions: Set<ModelCodeGenOptionType>,
    private val clientOptions: Set<ClientCodeGenOptionType>,
    private val clientTarget: ClientCodeGenTargetType,
    private val typeOverrides: Set<CodeGenTypeOverride>,
    private val validationLibrary: ValidationLibrary,
    private val serializationLibrary: SerializationLibrary,
    private val instantLibrary: InstantLibrary,
) {
    fun resolve(): List<DependencyNotation> =
        buildList {
            addAll(resolveModelsDependencies())
            addAll(resolveClientDependencies())
            addAll(resolveControllerDependencies())
        }.distinct()

    private fun resolveControllerDependencies(): List<DependencyNotation> =
        takeIf { generatorTypes.contains(CodeGenerationType.CONTROLLERS) }
            ?.let {
                buildList {
                    add(controllerTarget)
                    addAll(controllerOptions)
                }
            }?.flatMap { it.requiredDependencies() }
            ?: emptyList()

    private fun resolveClientDependencies(): List<DependencyNotation> =
        takeIf { generatorTypes.contains(CodeGenerationType.CLIENT) }
            ?.let {
                buildList {
                    add(clientTarget)
                    addAll(clientOptions)
                }
            }?.flatMap { it.requiredDependencies() }
            ?: emptyList()

    private fun resolveModelsDependencies(): List<DependencyNotation> =
        buildList {
            addAll(modelOptions)
            add(validationLibrary)
            add(serializationLibrary)
            addAll(typeOverrides)
            if (CodeGenTypeOverride.DATETIME_AS_INSTANT in typeOverrides) {
                add(instantLibrary)
            }
        }.flatMap { it.requiredDependencies() }

    companion object {
        fun withCurrentSettings(): DependenciesResolver =
            DependenciesResolver(
                generatorTypes = MutableSettings.generationTypes,
                controllerOptions = MutableSettings.controllerOptions,
                controllerTarget = MutableSettings.controllerTarget,
                modelOptions = MutableSettings.modelOptions,
                clientOptions = MutableSettings.clientOptions,
                clientTarget = MutableSettings.clientTarget,
                typeOverrides = MutableSettings.typeOverrides,
                validationLibrary = MutableSettings.validationLibrary,
                serializationLibrary = MutableSettings.serializationLibrary,
                instantLibrary = MutableSettings.instantLibrary,
            )
    }
}
