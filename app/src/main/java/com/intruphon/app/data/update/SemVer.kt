package com.intruphon.app.data.update

/** True when [candidate] is a higher SemVer version than [current]. A leading "v" and any pre-release or build suffix are ignored. */
fun isNewer(candidate: String, current: String): Boolean {
    val a = parts(candidate) ?: return false
    val b = parts(current) ?: return false
    for (i in 0..2) if (a[i] != b[i]) return a[i] > b[i]
    return false
}

private fun parts(version: String): List<Int>? {
    val core = version.trim().removePrefix("v").substringBefore('-').substringBefore('+')
    val pieces = core.split('.')
    if (pieces.size != 3) return null
    return pieces.map { it.toIntOrNull() ?: return null }
}
