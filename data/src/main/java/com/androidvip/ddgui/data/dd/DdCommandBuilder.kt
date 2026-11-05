package com.androidvip.ddgui.data.dd

import com.androidvip.ddgui.domain.dd.DdRequest

internal object DdCommandBuilder {
    fun build(request: DdRequest): String {
        require(request.inputPath.isNotEmpty()) { "Input path must not be empty" }
        require(request.outputPath.isNotEmpty()) { "Output path must not be empty" }
        require('\u0000' !in request.inputPath) { "Input path contains a null character" }
        require('\u0000' !in request.outputPath) { "Output path contains a null character" }

        return buildString {
            append("dd if=").append(quotePath(request.inputPath))
            append(" of=").append(quotePath(request.outputPath))

            val conversions = request.conversions.distinct()
            if (conversions.isNotEmpty()) {
                append(" conv=")
                append(conversions.joinToString(",") { it.argument })
            }
        }
    }

    private fun quotePath(path: String): String = "'${path.replace("'", "'\\''")}'"
}
