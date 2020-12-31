package net.babys_care.app.utils.version

import java.util.*
import kotlin.collections.ArrayList

class Version(private val mainVersionNumber: String?): Comparable<Version> {

    private val snapShotString = "snapshot"

    private val snapshot = 0
    private val unknown = 5

    private val subversionNumbers: MutableList<Int> = ArrayList()

    private val subversionNumbersWithoutTrailingZeros: MutableList<Int> = ArrayList()

    private var suffix = ""

    init {
        initVersion()
    }

    /**
     * Checks if the Version object is higher than `otherVersion`.
     *
     * @param otherVersion a string representing another version.
     * @return `true` if Version object is higher than `otherVersion` or `otherVersion` could not get
     * parsed. `False` if the Version is lower or equal.
     */
    fun isHigherThan(otherVersion: String?): Boolean {
        return isHigherThan(Version(otherVersion))
    }

    private fun isHigherThan(otherVersion: Version): Boolean {
        val subversionResult: Int = compareSubversionNumbers(
            subversionNumbersWithoutTrailingZeros,
            otherVersion.subversionNumbersWithoutTrailingZeros
        )
        return if (subversionResult != 0) {
            subversionResult > 0
        } else compareSuffix(suffix, otherVersion.suffix) > 0
    }

    private fun isLowerThan(otherVersion: Version): Boolean {
        val subversionResult: Int = compareSubversionNumbers(
            subversionNumbersWithoutTrailingZeros,
            otherVersion.subversionNumbersWithoutTrailingZeros
        )
        return if (subversionResult != 0) {
            subversionResult < 0
        } else compareSuffix(suffix, otherVersion.suffix) < 0
    }

    private fun isEqual(otherVersion: Version): Boolean {
        return compareSubversionNumbers(
            subversionNumbersWithoutTrailingZeros,
            otherVersion.subversionNumbersWithoutTrailingZeros
        ) == 0 && compareSuffix(suffix, otherVersion.suffix) == 0
    }

    private fun initVersion() {
        val originalString = this.mainVersionNumber ?: return
        if (startsNumeric(originalString)) {
            val versionTokens =
                originalString.replace("\\s".toRegex(), "").split("\\.".toRegex()).toTypedArray()
            var suffixFound = false
            var suffixSb: StringBuilder? = null
            for (versionToken in versionTokens) {
                if (suffixFound) {
                    suffixSb!!.append(".")
                    suffixSb.append(versionToken)
                } else if (isNumeric(versionToken)) {
                    subversionNumbers.add(safeParseInt(versionToken))
                } else {
                    for (i in versionToken.indices) {
                        if (!Character.isDigit(versionToken[i])) {
                            suffixSb = StringBuilder()
                            if (i > 0) {
                                subversionNumbers.add(
                                    safeParseInt(
                                        versionToken.substring(
                                            0,
                                            i
                                        )
                                    )
                                )
                                suffixSb.append(versionToken.substring(i))
                            } else {
                                suffixSb.append(versionToken)
                            }
                            suffixFound = true
                            break
                        }
                    }
                }
            }
            subversionNumbersWithoutTrailingZeros.addAll(subversionNumbers)
            while (subversionNumbersWithoutTrailingZeros.isNotEmpty() &&
                subversionNumbersWithoutTrailingZeros.lastIndexOf(0) == subversionNumbersWithoutTrailingZeros.size - 1
            ) {
                subversionNumbersWithoutTrailingZeros.removeAt(
                    subversionNumbersWithoutTrailingZeros.lastIndexOf(
                        0
                    )
                )
            }
            if (suffixSb != null) suffix = suffixSb.toString()
        }
    }

    override fun compareTo(other: Version): Int {
        return if (this.isEqual(other)) 0 else if (this.isLowerThan(other)) -1 else 1
    }

    override fun equals(other: Any?): Boolean {
        return if (other is Version && this.isEqual(other)) true else super.equals(other)
    }

    override fun hashCode(): Int {
        val prime = 31
        var hash = 1
        hash = prime * hash + subversionNumbersWithoutTrailingZeros.hashCode()
        if (suffix.isEmpty()) return hash
        val releaseQualifier: Int = qualifierToNumber(suffix)
        val releaseQualifierVersion: Int = preReleaseVersion(suffix)
        hash = prime * hash + releaseQualifier
        hash = prime * hash + releaseQualifierVersion
        return hash
    }

    private fun compareSubversionNumbers(
        subversionA: List<Int>,
        subversionB: List<Int>
    ): Int {
        val verASize = subversionA.size
        val verBSize = subversionB.size
        val maxSize = verASize.coerceAtLeast(verBSize)
        for (i in 0 until maxSize) {
            if ((if (i < verASize) subversionA[i] else 0) > (if (i < verBSize) subversionB[i] else 0)) {
                return 1
            } else if ((if (i < verASize) subversionA[i] else 0) < (if (i < verBSize) subversionB[i] else 0)) {
                return -1
            }
        }
        return 0
    }

    private fun compareSuffix(suffixA: String, suffixB: String): Int {
        if (suffixA.isNotEmpty() || suffixB.isNotEmpty()) {
            val qualifierA = qualifierToNumber(suffixA)
            val qualifierB = qualifierToNumber(suffixB)
            if (qualifierA > qualifierB) {
                return 1
            } else if (qualifierA < qualifierB) {
                return -1
            } else if (qualifierA != unknown && qualifierA != snapshot) {
                val suffixVersionA = preReleaseVersion(suffixA)
                val suffixVersionB = preReleaseVersion(suffixB)
                if (suffixVersionA > suffixVersionB) {
                    return 1
                } else if (suffixVersionA < suffixVersionB) {
                    return -1
                }
            }
        }
        return 0
    }

    private fun qualifierToNumber(suffix: String): Int {
        var suffixLocal = suffix
        if (suffixLocal.isNotEmpty()) {
            suffixLocal = suffixLocal.toLowerCase(Locale.ROOT)
            if (suffixLocal.contains(snapShotString)) return snapshot
        }
        return unknown
    }

    private fun preReleaseVersion(suffix: String): Int {
        val startIndex = 0
        if (startIndex < suffix.length) {
            val maxStartIndex = (startIndex + 2).coerceAtMost(suffix.length)
            if (containsNumeric(suffix.substring(startIndex, maxStartIndex))) {
                val versionNumber = StringBuilder()
                for (i in startIndex until suffix.length) {
                    val c = suffix[i]
                    if (Character.isDigit(c)) {
                        versionNumber.append(c)
                    } else if (i != startIndex) {
                        break
                    }
                }
                return safeParseInt(versionNumber.toString())
            }
        }
        return 0
    }

    private fun startsNumeric(string: String): Boolean {
        var str = string
        str = str.trim { it <= ' ' }
        return str.isNotEmpty() && Character.isDigit(str[0])
    }

    private fun safeParseInt(num: String): Int {
        var numbers = num
        if (numbers.length > 9) {
            numbers = numbers.substring(0, 9)
        }
        return numbers.toInt()
    }

    private fun isNumeric(cs: CharSequence): Boolean {
        val sz = cs.length
        if (sz > 0) {
            for (i in 0 until sz) {
                if (!Character.isDigit(cs[i])) {
                    return false
                }
            }
            return true
        }
        return false
    }

    private fun containsNumeric(cs: CharSequence): Boolean {
        val sz = cs.length
        if (sz > 0) {
            for (i in 0 until sz) {
                if (Character.isDigit(cs[i])) {
                    return true
                }
            }
        }
        return false
    }
}