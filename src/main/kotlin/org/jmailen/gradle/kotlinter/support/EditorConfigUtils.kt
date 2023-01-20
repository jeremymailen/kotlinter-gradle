package org.jmailen.gradle.kotlinter.support

import com.pinterest.ktlint.core.api.EditorConfigDefaults
import com.pinterest.ktlint.core.api.EditorConfigOverride
import com.pinterest.ktlint.core.api.editorconfig.EditorConfigProperty
import org.ec4j.core.model.EditorConfig
import org.ec4j.core.model.Glob
import org.ec4j.core.model.Property
import org.ec4j.core.model.PropertyType
import org.ec4j.core.model.PropertyType.PropertyValueParser.IDENTITY_VALUE_PARSER
import org.ec4j.core.model.Section
import org.gradle.api.file.ProjectLayout
import java.io.File

internal fun editorConfigOverride(ktLintParams: KtLintParams) =
    getPropertiesForDisabledRules(ktLintParams)
        .let(::buildEditorConfigOverride)

internal fun editorConfigDefaults(ktLintParams: KtLintParams): EditorConfigDefaults =
    getPropertiesForExperimentalRules(ktLintParams)
        .let(::buildEditorConfigDefaults)

private fun getPropertiesForDisabledRules(
    ktLintParams: KtLintParams,
): List<Pair<EditorConfigProperty<String>, String>> {
    val rules = ktLintParams.disabledRules
    return if (rules.isEmpty()) {
        emptyList()
    } else {
        rules
            .asSequence()
            .map(::getKtlintRulePropertyName)
            .map { propertyName ->
                EditorConfigProperty(
                    type = PropertyType(propertyName, "Rule to be disabled", IDENTITY_VALUE_PARSER),
                    defaultValue = "disabled",
                )
            }
            .map { it to "disabled" }
            .toList()
    }
}

private fun getPropertiesForExperimentalRules(ktLintParams: KtLintParams) =
    Property
        .builder()
        .name("ktlint_experimental")
        .value(if (ktLintParams.experimentalRules) "enabled" else "disabled")
        .let(::listOf)

private fun buildEditorConfigOverride(editorConfigProperties: List<Pair<EditorConfigProperty<String>, String>>) =
    if (editorConfigProperties.isEmpty()) {
        EditorConfigOverride.EMPTY_EDITOR_CONFIG_OVERRIDE
    } else {
        EditorConfigOverride.from(*editorConfigProperties.toTypedArray())
    }

private fun buildEditorConfigDefaults(kotlinSectionProperties: List<Property.Builder>) =
    if (kotlinSectionProperties.isEmpty()) {
        EditorConfigDefaults.EMPTY_EDITOR_CONFIG_DEFAULTS
    } else {
        EditorConfigDefaults(
            EditorConfig
                .builder()
                .section(
                    Section
                        .builder()
                        .glob(Glob("*.{kt,kts}"))
                        .properties(kotlinSectionProperties),
                )
                .build(),
        )
    }

private fun getKtlintRulePropertyName(ruleName: String) =
    if (ruleName.contains(':')) { // Rule from a non-standard rule set
        "ktlint_${ruleName.replace(':', '_')}"
    } else {
        "ktlint_standard_$ruleName"
    }

internal fun ProjectLayout.findApplicableEditorConfigFiles(): Sequence<File> {
    val projectEditorConfig = projectDirectory.file(".editorconfig").asFile

    return generateSequence(seed = projectEditorConfig) { editorconfig ->
        if (editorconfig.isRootEditorConfig) {
            null
        } else {
            editorconfig.parentFile?.parentFile?.resolve(".editorconfig")
        }
    }
}

private val File.isRootEditorConfig: Boolean
    get() {
        if (!isFile || !canRead()) return false

        return useLines { lines ->
            lines.any { line -> line.matches(editorConfigRootRegex) }
        }
    }

/**
 * According to https://editorconfig.org/ root-most EditorConfig file contains line with `root=true`
 */
private val editorConfigRootRegex = "^root\\s?=\\s?true".toRegex()
