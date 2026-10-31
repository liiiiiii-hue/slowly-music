package com.slowly.lyrics

data class Word(val timeMs: Long, val text: String)
data class Line(val timeMs: Long, val text: String, val words: List<Word> = emptyList())
object Lrc {
    private val stamp = Regex("\\[(\\d+):([0-5]\\d)(?:[.:](\\d{1,3}))?]")
    private val word = Regex("<(\\d+):([0-5]\\d)(?:[.:](\\d{1,3}))?>")
    private fun time(m: MatchResult): Long = m.groupValues[1].toLong() * 60000 + m.groupValues[2].toLong() * 1000 + m.groupValues[3].padEnd(3, '0').take(3).toLong()
    fun parse(raw: String): List<Line> {
        val offset = Regex("\\[offset:([+-]?\\d+)]", RegexOption.IGNORE_CASE).find(raw)?.groupValues?.get(1)?.toLongOrNull() ?: 0
        return raw.removePrefix("\uFEFF").lineSequence().flatMap { row ->
            val stamps = stamp.findAll(row).toList()
            if (stamps.isEmpty()) return@flatMap emptySequence()
            val body = row.substring(stamps.last().range.last + 1)
            val marks = word.findAll(body).toList()
            val words = marks.mapIndexed { i, m -> Word((time(m) - offset).coerceAtLeast(0), body.substring(m.range.last + 1, marks.getOrNull(i + 1)?.range?.first ?: body.length)) }
            val validWords = words.takeIf { it.isNotEmpty() && it.zipWithNext().all { (a,b) -> a.timeMs <= b.timeMs } } ?: emptyList()
            stamps.asSequence().map { Line((time(it) - offset).coerceAtLeast(0), word.replace(body, ""), validWords) }
        }.sortedBy { it.timeMs }.toList()
    }
    fun current(lines: List<Line>, position: Long): Int = lines.indexOfLast { it.timeMs <= position }
}
sealed interface Match {
    data class Selected(val path: String): Match
    data class Conflict(val paths: List<String>): Match
    data object Missing: Match
}
fun matchLyrics(song: String, files: List<String>, manual: String? = null): Match {
    if (manual != null && manual in files) return Match.Selected(manual)
    val name = song.substringAfterLast('/').substringBeforeLast('.')
    val candidates = files.filter { it.substringAfterLast('.').equals("lrc", true) && it.substringAfterLast('/').substringBeforeLast('.').equals(name, true) }
    return when(candidates.size) { 0 -> Match.Missing; 1 -> Match.Selected(candidates.single()); else -> Match.Conflict(candidates) }
}
