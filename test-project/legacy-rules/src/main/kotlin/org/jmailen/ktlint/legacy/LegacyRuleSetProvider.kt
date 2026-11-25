@file:Suppress("DEPRECATION")

package org.jmailen.ktlint.legacy

import com.intellij.lang.ASTNode
import com.pinterest.ktlint.cli.ruleset.core.api.RuleSetProviderV3
import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleAutocorrectApproveHandler
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.rule.engine.core.api.RuleSetId
import org.jetbrains.kotlin.psi.KtPsiFactory

class LegacyRuleSetProvider : RuleSetProviderV3(RuleSetId("legacy-test-rules")) {
    override fun getRuleProviders(): Set<RuleProvider> = setOf(RuleProvider { LegacyRule() })
}

class LegacyRule : Rule(RuleId("legacy-test-rules:no-var"), About("fixture")), RuleAutocorrectApproveHandler {
    override fun beforeVisitChildNodes(node: ASTNode, emit: (Int, String, Boolean) -> AutocorrectDecision) {
        if (node.elementType == ElementType.VAR_KEYWORD && node.treeParent.text.contains("legacyValue")) {
            if (emit(node.startOffset, "Legacy rule detected var", true) == AutocorrectDecision.ALLOW_AUTOCORRECT) {
                val valKeyword = KtPsiFactory(node.psi.project).createProperty("val example = 1")
                    .node.findChildByType(ElementType.VAL_KEYWORD)!!
                node.treeParent.replaceChild(node, valKeyword)
            }
        }
    }
}
