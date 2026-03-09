package com.github.methodtimer.plugin.util

import com.intellij.psi.PsiMethod
import com.intellij.psi.util.TypeConversionUtil

object MethodSignatureResolver {

    fun resolveSignature(method: PsiMethod): String? {
        val containingClass = method.containingClass ?: return null
        val className = containingClass.qualifiedName ?: return null
        val params = method.parameterList.parameters.joinToString(", ") { param ->
            TypeConversionUtil.erasure(param.type).canonicalText
        }
        return "$className.${method.name}($params)"
    }
}
