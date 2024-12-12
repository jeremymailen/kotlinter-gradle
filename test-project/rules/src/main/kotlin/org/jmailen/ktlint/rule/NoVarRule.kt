package org.jmailen.ktlint.rule

import com.pinterest.ktlint.rule.engine.core.api.*
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

class NoVarRule :
    Rule(
        ruleId = RuleId("$CUSTOM_RULE_SET_ID:no-var"),
        about = About(
            maintainer = "jmailen",
            repositoryUrl = "https://gtihub.com/jeremymailen/kotlinter-gradle"
        )
    ),
    RuleAutocorrectApproveHandler {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision
    ) {
        if (node.elementType == ElementType.VAR_KEYWORD) {
            emit(node.startOffset, "Use val instead of var", false)
        }
    }
}
