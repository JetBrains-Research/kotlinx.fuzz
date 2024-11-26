package org.plan.research

object MyUtils {

    @JvmStatic
    fun resultThrowIsFailure(result: Any?) {
        val r = result as? Result<*> ?: return
        if (r.isFailure) throw r.exceptionOrNull()!!
    }

}