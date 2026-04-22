package com.adamglin.recompose.pulse.compiler

data class PulseCompilerOptions(
    val enabled: Boolean,
    val includePackages: Set<String>,
    val excludePackages: Set<String>,
) {
    fun shouldTransform(packageFqName: String): Boolean {
        if (excludePackages.any { packageFqName.isSamePackageOrSubpackageOf(it) }) {
            return false
        }

        if (includePackages.isEmpty()) {
            return true
        }

        return includePackages.any { packageFqName.isSamePackageOrSubpackageOf(it) }
    }

    private fun String.isSamePackageOrSubpackageOf(other: String): Boolean {
        return this == other || startsWith("${other}.")
    }
}
