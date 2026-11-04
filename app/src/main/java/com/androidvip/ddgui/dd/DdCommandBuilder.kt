package com.androidvip.ddgui.dd

internal enum class DdConversion(val argument: String) {
    NO_TRUNCATE("notrunc"),
    NO_ERROR("noerror"),
    SYNC("sync"),
    FSYNC("fsync"),
    SWAB("swab")
}

internal data class DdRequest(
    val inputPath: String,
    val outputPath: String,
    val conversions: List<DdConversion>
)

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
