package com.example.ui.theme

/**
 * Every release, newest first: the single source for the "What's new" sheet/page.
 * `release-notes.md` must repeat the newest entry's Arabic points (checked by ChangelogTest).
 */
data class ChangelogEntry(val version: String, val ar: List<String>, val en: List<String>) {
    fun points(arabic: Boolean) = if (arabic) ar else en
}

object Changelog {
    val entries = listOf(
        ChangelogEntry(
            "3.3.0",
            ar = listOf(
                "النسخ الاحتياطي المضغوط: خيار «ضغط النسخة (ZIP)» مفعّل افتراضياً، فتأخذ النسخة مساحة أقل.",
                "الأمان قبل المساحة: كل ملف يُفحص ببصمة SHA-256 بعد الحفظ، ومرة أخرى أثناء الاستعادة قبل التثبيت، والملف التالف لا يُثبَّت أبداً.",
                "الاستعادة تقرأ من الملف المضغوط مباشرة دون فكّه، فلا تحتاج مساحة إضافية. والنسخ القديمة بمجلدات ما زالت تُستعاد كما هي.",
                "تقرير بعد التثبيت الجماعي بثلاث قوائم: نجح، وفشل مع سببه وزر «أعد المحاولة»، ومتخطى مع «ثبّت الآن» و«تجاهل» و«المتجر».",
                "تثبيت جماعي صامت دون ضغطة لكل تطبيق عند تشغيل تطبيق Shizuku (اختياري)."
            ),
            en = listOf(
                "Compressed backups: “Compress the backup (ZIP)” is on by default, so backups take less space.",
                "Safety before space: every file is checked by SHA-256 after saving and again while restoring, before installing; a damaged file is never installed.",
                "Restore reads straight from the archive without extracting it, so it needs no extra space. Older folder backups restore as before.",
                "A report after batch installs with three lists: installed, failed with the reason and Retry, and skipped with Install now, Ignore and Store.",
                "Silent batch installs, no tap per app, when the Shizuku app is running (optional)."
            )
        ),
        ChangelogEntry(
            "3.2.0",
            ar = listOf(
                "النقل عبر المتصفح: زر «من المتصفح» في تبويب الاستقبال يعطيك رابطاً ورمزاً من 4 أرقام لأي حاسوب ويندوز أو ماك على نفس الشبكة، بلا تثبيت.",
                "من الحاسوب: نزّل التطبيقات والملفات المستلمة وما تختاره في تبويب «الملفات»، وارفع ملفات ونصوصاً إلى الهاتف.",
                "حجم الخط من الإعدادات، وتصميم مناسب للأجهزة اللوحية والعرض الأفقي.",
                "بحث يتجاهل الفرق بين الهمزات والتاء المربوطة والألف المقصورة.",
                "نافذة «ما الجديد» بعد كل تحديث، وسجل تشخيص للأخطاء في «المزيد»، وأسباب واضحة لفشل الإرسال."
            ),
            en = listOf(
                "Transfer via browser: “From browser” in Receive gives any Windows or Mac computer on the same network a link and a 4-digit code, nothing to install.",
                "From the computer: download apps, received files and anything picked in the Files tab, and upload files and text to the phone.",
                "Text size in Settings, and layouts made for tablets and landscape.",
                "Search that ignores hamza, taa marbuta and alef maqsura differences.",
                "A “What's new” sheet after each update, a diagnostics log in More, and clear reasons when sending fails."
            )
        ),
        ChangelogEntry(
            "3.0.0",
            ar = listOf(
                "تصميم جديد بالكامل بأربعة تبويبات: التطبيقات، الملفات، استقبال، المزيد.",
                "متوافق مع LocalSend: إرسال واستقبال مباشر مع أي جهاز على نفس الشبكة.",
                "إرسال واستقبال الصور والفيديو والمجلدات والنصوص.",
                "التحديث من داخل التطبيق."
            ),
            en = listOf(
                "A complete redesign with four tabs: Apps, Files, Receive, More.",
                "LocalSend compatible: send and receive directly with any device on the same network.",
                "Send and receive photos, videos, folders and text.",
                "In-app updates."
            )
        )
    )

    val latest get() = entries.first()
}
