package com.scanprototype.scan

import java.util.Calendar

data class CallEvent(val timestamp: Long, val callerId: String, val cnam: String)

enum class Verdict {
    ALLOW,
    WARN,
    BLOCK
}

object HeuristicSettings {

    var timeWeight = 2
    var velocityWeight = 3
    var spoofWeight = 5
    var blacklistWeight = 10

    var warnThreshold = 4
    var blockThreshold = 7
}

data class SimulationResult(
    val number: String,
    val verdict: Verdict,
    val score: Int,
    val reason: String,
    val details: List<String>
)

object StorageLayer {
    var deviceNumber: String = ""

    val savedContacts: Set<String> = setOf(
        // placeholder numbers for testing whitelist scenarios
        "639171112222",
        "639189998888",
        "639171234567"
    )

    val blacklist: List<String> = listOf(

        // Bank impersonation scams
        "639178503081",
        "639178537084",
        "639479938401",
        "639688756584",
        "639171634761",

        // One-ring / robocaller scams
        "639479171846",
        "639479171928",
        "639479171947",
        "639479171979",
        "639178347233",
        "639178063674",

        // Numbers submitted for NTC blocking
        "639066778059",
        "639947289642",
        "639754424042",

        // Crowdsourced scam numbers
        "639661971392",
        "639102576320",
        "639850329362",
        "639952647035",
        "639166023529",
        "639623984341"
    )

    private val callHistoryLogs: MutableMap<String, MutableList<Long>> = mutableMapOf()

    fun logExecution(normalizedId: String, timestamp: Long) {
        val history = callHistoryLogs.getOrPut(normalizedId) { mutableListOf() }
        history.add(timestamp)
    }

    fun getRecentCallCount(normalizedId: String, referenceTime: Long): Int {
        val history = callHistoryLogs[normalizedId] ?: return 0
        val windowStart = referenceTime - 86_400_000L
        return history.count { it >= windowStart }
    }

    fun formatAuditLog(): String {
        if (callHistoryLogs.isEmpty()) {
            return "No logs yet"
        }
        val builder = StringBuilder()
        callHistoryLogs.forEach { (number, timestamps) ->
            builder.append("$number: ${timestamps.size} calls\n")
            timestamps.sorted().forEach { ts ->
                builder.append("  • ${ts}\n")
            }
        }
        return builder.toString().trimEnd()
    }
}

object DataNormalizer {
    fun normalize(rawId: String): String {
        var number = rawId.replace(Regex("[^0-9]"), "")

    if (number.startsWith("09")) {
        number = "63" + number.substring(1)
    }

        return number
}
}

fun process2_WhitelistCheck(normalizedId: String): Boolean {
    return StorageLayer.savedContacts.contains(normalizedId)
}

class BoyerMooreEngine {
    private val alphabetSize = 256

    fun scanAgainstBlacklist(text: String, patterns: List<String>): Boolean {
        return patterns.any { pattern -> executeBM(text, pattern) }
    }

    private fun executeBM(text: String, pattern: String): Boolean {
        val m = pattern.length
        val n = text.length
        if (m == 0 || m > n) return false

        val badChar = IntArray(alphabetSize) { -1 }
        for (i in 0 until m) {
            badChar[pattern[i].code] = i
        }

        val goodSuffix = IntArray(m + 1) { 0 }
        val bpos = IntArray(m + 1) { 0 }
        preprocessStrongSuffix(goodSuffix, bpos, pattern, m)
        preprocessCase2(goodSuffix, bpos, pattern, m)

        var s = 0
        while (s <= n - m) {
            var j = m - 1
            while (j >= 0 && pattern[j] == text[s + j]) {
                j--
            }
            if (j < 0) {
                return true
            } else {
                val badCharShift = j - badChar[text[s + j].code]
                val goodSuffixShift = goodSuffix[j + 1]
                s += maxOf(1, maxOf(badCharShift, goodSuffixShift))
            }
        }
        return false
    }

    private fun preprocessStrongSuffix(shift: IntArray, bpos: IntArray, pat: String, m: Int) {
        var i = m
        var j = m + 1
        bpos[i] = j
        while (i > 0) {
            while (j <= m && pat[i - 1] != pat[j - 1]) {
                if (shift[j] == 0) {
                    shift[j] = j - i
                }
                j = bpos[j]
            }
            i--
            j--
            bpos[i] = j
        }
    }

    private fun preprocessCase2(shift: IntArray, bpos: IntArray, pat: String, m: Int) {
        var j = bpos[0]
        for (i in 0..m) {
            if (shift[i] == 0) {
                shift[i] = j
            }
            if (i == j) {
                j = bpos[j]
            }
        }
    }
}

class HeuristicScoringEngine {
    private val w1
        get() = HeuristicSettings.timeWeight

    private val w2
        get() = HeuristicSettings.velocityWeight

    private val w3
        get() = HeuristicSettings.spoofWeight

    private val w4
        get() = HeuristicSettings.blacklistWeight

    fun computeRiskScore(event: CallEvent, normalizedId: String): Pair<Int, List<String>> {
        val details = mutableListOf<String>()
        var h1 = 0
        var h2 = 0
        var h3 = 0
        var h4 = 0

        val hour = getHourFromEpoch(event.timestamp)
        if (hour >= 23 || hour < 6) {
            h1 = 1
            details.add("Time-of-day anomaly detected (hour=$hour)")
        }

        val recentCalls = StorageLayer.getRecentCallCount(normalizedId, event.timestamp)
        if (recentCalls >= 2) {
            h2 = 1
            details.add("Call velocity anomaly detected ($recentCalls prior calls in 24h)")
        }

        val bmEngine = BoyerMooreEngine()

        if (
            bmEngine.scanAgainstBlacklist(
                normalizedId,
                StorageLayer.blacklist
            )
        ) {
            h4 = 1
            details.add(
                "Blacklist pattern matched"
            )
        }

        if (normalizedId.length >= 6 && StorageLayer.deviceNumber.length >= 6) {
            val incomingPrefix = normalizedId.substring(0, 6)
            val devicePrefix = StorageLayer.deviceNumber.substring(0, 6)
            if (incomingPrefix == devicePrefix) {
                h3 = 1
                details.add("Neighbor spoofing indicator matched for prefix $incomingPrefix")
            }
        }

        val score = (w1 * h1) + (w2 * h2) + (w3 * h3) + (w4 * h4)
        if (score == 0) {
            details.add("No heuristic anomalies detected")
        }
        return score to details
    }

    private fun getHourFromEpoch(epoch: Long): Int {
        return Calendar.getInstance().apply { timeInMillis = epoch }.get(Calendar.HOUR_OF_DAY)
    }
}

fun process6_ActionRouting(score: Int, normalizedId: String, timestamp: Long, details: List<String>): SimulationResult {
    StorageLayer.logExecution(normalizedId, timestamp)
    return when {
        score >= HeuristicSettings.blockThreshold -> SimulationResult(
            number = normalizedId,
            verdict = Verdict.BLOCK,
            score = score,
            reason = "Malicious threshold reached",
            details = listOf("Action: BLOCK") + details
        )
        score in HeuristicSettings.warnThreshold until
            HeuristicSettings.blockThreshold -> SimulationResult(
                number = normalizedId,
                verdict = Verdict.WARN,
                score = score,
                reason = "Borderline behavioral anomalies",
                details = listOf("Action: WARN") + details
        )
        else -> SimulationResult(
            number = normalizedId,
            verdict = Verdict.ALLOW,
            score = score,
            reason = "Safe threshold maintained",
            details = listOf("Action: ALLOW") + details
        )
    }
}

fun executeSimulationPipeline(event: CallEvent): SimulationResult {
    val normalizedId = DataNormalizer.normalize(event.callerId)
    if (normalizedId.isEmpty()) {
        return SimulationResult(
            number = event.callerId,
            verdict = Verdict.WARN,
            score = 0,
            reason = "Invalid normalized number",
            details = listOf("Unable to normalize input call ID")
        )
    }

    if (process2_WhitelistCheck(normalizedId)) {
        return SimulationResult(
            number = normalizedId,
            verdict = Verdict.ALLOW,
            score = 0,
            reason = "Whitelist optimization matched",
            details = listOf("Whitelist bypass: saved contact matched")
        )
    }

    val (score, heuristicDetails) = HeuristicScoringEngine().computeRiskScore(event, normalizedId)
    return process6_ActionRouting(score, normalizedId, event.timestamp, heuristicDetails)
}
