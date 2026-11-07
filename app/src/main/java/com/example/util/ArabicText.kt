package com.example.util

/** Pure text helpers (no android.*), unit-tested on the JVM. */
object ArabicText {

    /**
     * Forms of a counted noun: [one] "تطبيق واحد", [two] "تطبيقان", [few] (3–10) "تطبيقات",
     * [many] (11–99) "تطبيقاً", [other] (0, 100+) "تطبيق". English uses [one] and [other] only.
     */
    data class Forms(val one: String, val two: String, val few: String, val many: String, val other: String)

    /** "تطبيق واحد"، "تطبيقان"، "3 تطبيقات"، "11 تطبيقاً"، "100 تطبيق"، "0 تطبيقات". */
    fun count(n: Int, f: Forms, arabic: Boolean = true): String {
        if (!arabic) return if (n == 1) "1 ${f.one}" else "$n ${f.other}"
        val mod100 = n % 100
        return when {
            n == 0 -> "0 ${f.few}"
            n == 1 -> f.one
            n == 2 -> f.two
            mod100 in 3..10 -> "$n ${f.few}"
            mod100 in 11..99 -> "$n ${f.many}"
            else -> "$n ${f.other}"
        }
    }

    private val TASHKEEL = Regex("[\\u064B-\\u065F\\u0670\\u0640]")

    /** Folds the spellings people mix up, so «ادلب» finds «إدلب» and «محاضره» finds «محاضرة». */
    fun normalizeForSearch(s: String): String {
        val sb = StringBuilder(s.length)
        for (ch in TASHKEEL.replace(s, "")) {
            sb.append(
                when (ch) {
                    'أ', 'إ', 'آ', 'ٱ' -> 'ا'
                    'ة' -> 'ه'
                    'ى' -> 'ي'
                    'ؤ' -> 'و'
                    'ئ' -> 'ي'
                    in '٠'..'٩' -> '0' + (ch - '٠')
                    in '۰'..'۹' -> '0' + (ch - '۰')
                    else -> ch.lowercaseChar()
                }
            )
        }
        return sb.toString().trim()
    }

    /** True if every word of [query] appears in [text], after folding both. */
    fun matches(text: String, query: String): Boolean {
        val words = normalizeForSearch(query).split(' ').filter { it.isNotEmpty() }
        if (words.isEmpty()) return true
        val hay = normalizeForSearch(text)
        return words.all { hay.contains(it) }
    }

    /** Release notes as plain text: drops `#` headings and `**`, turns `-`/`*` list items into bullets. */
    fun plainNotes(md: String): String = md.lines().joinToString("\n") { line ->
        val t = line.replace("**", "").replace("`", "")
        val trimmed = t.trimStart()
        val indent = t.length - trimmed.length
        when {
            trimmed.startsWith("#") -> trimmed.trimStart('#').trim()
            trimmed.startsWith("- ") || trimmed.startsWith("* ") -> " ".repeat(indent) + "• " + trimmed.substring(2)
            else -> t
        }
    }.trim()
}
