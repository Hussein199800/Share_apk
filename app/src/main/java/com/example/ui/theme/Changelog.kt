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
            "3.2.0",
            ar = listOf(
                "تبويب «الشاشة» الجديد: شارك شاشتك مع جوال قريب ليساعدك، فيرى شاشتك ويشير بإصبعه أو يرسم عليها لتعرف أين تضغط، دون أن يتحكم بشيء.",
                "النقل عبر المتصفح: زر «من المتصفح» في تبويب الاستقبال يعطيك رابطاً ورمزاً من 4 أرقام لأي حاسوب ويندوز أو ماك على نفس الشبكة، بلا تثبيت.",
                "من الحاسوب: نزّل التطبيقات والملفات المستلمة وما تختاره في تبويب «الملفات»، وارفع ملفات ونصوصاً إلى الهاتف.",
                "حجم الخط من الإعدادات، وتصميم مناسب للأجهزة اللوحية والعرض الأفقي.",
                "بحث يتجاهل الفرق بين الهمزات والتاء المربوطة والألف المقصورة.",
                "نافذة «ما الجديد» بعد كل تحديث، وسجل تشخيص للأخطاء في «المزيد»، وأسباب واضحة لفشل الإرسال."
            ),
            en = listOf(
                "New Screen tab: share your screen with a nearby phone so a helper can see it and point or draw on it to show you where to tap, without controlling anything.",
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
