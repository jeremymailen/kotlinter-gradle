package org.jmailen.ktlint.rule

import io.github.ktlint.core.rule.engine.core.api.*
import com.intellij.lang.ASTNode

class NoVarRule :
    RuleV2(
        ruleId = RuleId("$CUSTOM_RULE_SET_ID:no-var"),
        about = About(
            maintainer = "jmailen",
            repositoryUrl = "https://gtihub.com/jeremymailen/kotlinter-gradle"
        )
    ) {
    override fun beforeVisitChildNodes(node: ASTNode) {
        if (node.elementType == ElementType.VAR_KEYWORD && !node.treeParent.text.contains("legacyValue")) {
            emit(node.startOffset, "Use val instead of var")
        }
    }
}
