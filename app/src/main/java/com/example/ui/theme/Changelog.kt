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
            "3.3.1",
            ar = listOf(
                "الاستعادة الجماعية أدق: كل نتيجة تثبيت تُربط بتطبيقها، فلا يُسجَّل تطبيق في القائمة الخطأ عند التخطي أو الإلغاء، والتطبيق الذي ثُبّت بعد تخطّيه يظهر في «نجح».",
                "واجهة أوضح: «النسخ والاستعادة» تشرح أن النسخة ملفات تثبيت فقط وتعرض عدد التطبيقات السليمة وما يحتاج إعادة نسخ، وزر الإرسال يسألك: جهاز قريب أم متصفح حاسوب، و«المزيد» يبرز أهم أداتين."
            ),
            en = listOf(
                "More accurate batch restore: each install result is tied to its own app, so skipping or cancelling never puts an app in the wrong list, and an app installed after being skipped shows as installed.",
                "Clearer screens: Backup & restore explains it saves install files only and shows how many apps are sound and which need copying again, Send asks: a nearby device or a computer's browser, and More highlights the two main tools."
            )
        ),
        ChangelogEntry(
            "3.3.0",
            ar = listOf(
                "النسخ الاحتياطي المضغوط: خيار «ضغط النسخة (ZIP)» مفعّل افتراضياً، فتأخذ النسخة مساحة أقل.",
                "الأمان قبل المساحة: كل ملف يُفحص ببصمة SHA-256 بعد الحفظ، ومرة أخرى أثناء الاستعادة قبل التثبيت، والملف التالف لا يُثبَّت أبداً.",
                "الاستعادة تقرأ من الملف المضغوط مباشرة دون فكّه، فلا تحتاج مساحة إضافية. والنسخ القديمة بمجلدات ما زالت تُستعاد كما هي.",
                "تقرير بعد التثبيت الجماعي بثلاث قوائم: نجح، وفشل مع سببه وزر «أعد المحاولة»، ومتخطى مع «ثبّت الآن» و«تجاهل» و«المتجر».",
                "تثبيت جماعي صامت دون ضغطة لكل تطبيق عند تشغيل تطبيق Shizuku (اختياري).",
                "إزالة التطبيقات المحددة دفعة واحدة من شريط التحديد. مع Shizuku تُزال تطبيقات النظام العنيدة للمستخدم الحالي أو تُعطَّل، ويمكن استرجاعها من «المزيد ← التطبيقات المحذوفة». التطبيقات الحساسة محمية من الإزالة.",
                "النسخ الاحتياطي: خانة «تحديد الكل» في الإنشاء والاستعادة، ورسالة واضحة إن فشلت النسخة أو لم تكفِ المساحة، وإصلاح أخطاء في عدّ المحدد وحجمه.",
                "نقل أدق: «حُفظ على الهاتف ✓» لا يظهر في المتصفح إلا بعد الحفظ الفعلي، مع السبب وزر «أعد المحاولة» عند الفشل. والملف الناقص لا يُقبل من LocalSend، والملفات بحجم مجهول (مثل Google Drive) تُرسل كاملة.",
                "نسخ احتياطي أوثق: التطبيق الذي تعذّر نسخ أحد ملفاته يُذكر باسمه ولا يُضاف للنسخة، ويُعاد قراءة ملف الفهرس من مكان الحفظ للتأكد، ولا يُعرض للاستعادة تطبيق بلا ملفه الأساسي."
            ),
            en = listOf(
                "Compressed backups: “Compress the backup (ZIP)” is on by default, so backups take less space.",
                "Safety before space: every file is checked by SHA-256 after saving and again while restoring, before installing; a damaged file is never installed.",
                "Restore reads straight from the archive without extracting it, so it needs no extra space. Older folder backups restore as before.",
                "A report after batch installs with three lists: installed, failed with the reason and Retry, and skipped with Install now, Ignore and Store.",
                "Silent batch installs, no tap per app, when the Shizuku app is running (optional).",
                "Remove the selected apps in one go from the selection bar. With Shizuku, stubborn system apps are removed for this user or disabled and can be brought back from More → Removed apps. Critical apps are protected.",
                "Backup: a Select all checkbox in create and restore, a clear message when a backup fails or space runs short, and fixes to the selected count and size.",
                "More reliable transfers: “Saved on the phone ✓” appears in the browser only once the file is really saved, with the reason and Retry on failure. Incomplete files from LocalSend are refused, and files of unknown size (like Google Drive) are sent whole.",
                "More trustworthy backups: an app whose files couldn't all be copied is named and left out, the index file is read back from the destination to confirm it, and apps without their base file are never offered for restore."
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
