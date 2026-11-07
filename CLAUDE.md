# شارك أبك — ملاحظات للمطورين ونماذج الذكاء الاصطناعي

- **الواجهة:** عربية أولاً (RTL)، مع ترجمة إنجليزية. كل نص يراه المستخدم يُضاف إلى `AppStrings` في `ui/theme/Strings.kt` بالنسختين `ArabicStrings` و`EnglishStrings` معاً، ولا تُكتب نصوص داخل الشاشات.
- **الألوان والأبعاد:** تُكتب في `ui/theme/` فقط (`Color.kt`، `Gradients.kt`، `ShareApkTokens.kt`). لا يُكتب `Color(0x…)` خارجها. المكوّنات المشتركة في `ui/components/design/`.
- **الأرقام وأسماء الحزم والإصدارات والعناوين:** تُعرض داخل `LtrBox`.
- **التبويبات:** `APPS · FILES · RECEIVE · MORE` (`MainNavTab`). الإرسال نافذة `DevicePickerSheet` تُفتح من التطبيقات أو الملفات. ترتيب زر الرجوع في `ui/navigation/NavState.kt`.
- **من المتصفح:** `web/` خادم HTTP على المنفذ 53320 يحميه رمز من 4 أرقام (`PinGate`). نصوص الصفحة داخل `WebPage.kt` لأنها تُعرض في متصفح الحاسوب.
- **الشبكة:**
  - متوافقة مع LocalSend v2 (`receive/`، `send/`).
  - المنفذ 53317 يخدم HTTPS وHTTP معاً عبر `OptionalSslHandler`، فلا تُرجعه إلى HTTPS فقط، لأن LocalSend بلا تشفير يتوقف عن العمل.
  - البصمة = SHA-256 لشهادة DER.
  - المرسل يتحقق من هوية المستلم: «شارك أبك» تحقق صارم، وLocalSend تثبيت عند أول اتصال (`PinStore`).
- **الاستقبال:**
  - التطبيقات تُحفظ في `filesDir/received`.
  - باقي الملفات تُنقل إلى `Download/ShareAPK` عبر MediaStore.
  - النصوص تصل في `preview`، والطلب الذي فيه نصوص فقط يُرد عليه بـ204.
  - مفتاح TLS والملفات المستلمة مستثناة من النسخ الاحتياطي.
- **الإصدار:** `versionName` و`versionCode` في `app/build.gradle.kts` (`versionCode` = رئيسي×10000 + فرعي×100 + تصحيح). مع كل إصدار: أضف مدخلاً في أول `ui/theme/Changelog.kt` بالعربية والإنجليزية، وانسخ نقاطه العربية إلى `release-notes.md` **نصاً عادياً بلا Markdown** (سطر العنوان ثم نقاط «•»). `ChangelogTest` يتحقق من تطابق الثلاثة. لا يُدمج في `main` دون رفع الإصدار.
- **النصوص الجديدة:** `AppStrings` وصل حد 255 معاملاً في JVM، فالنصوص الجديدة تُضاف إلى `ExtraStrings` (الحقل `x`) مع خاصية تمرير في `AppStrings`.
- **العدّ والبحث:** `util/ArabicText` (`count` لصياغة المعدود، `matches` للبحث الموحّد للحروف). الأخطاء تُصنَّف بـ`ErrorClassifier` وتُعرض من `errorReasons`، والرسالة الخام في `DiagnosticsLog`.
- **التحديث من داخل التطبيق:** يقرأ `releases/latest` من `Hussein199800/Share_apk`، ولا يثبّت إلا إذا تطابقت الحزمة والتوقيع والإصدار أحدث.
- **التوقيع:**
  - مفتاح التوقيع لا يُرفع أبداً.
  - الأسرار: `SHAREAPK_KEYSTORE_BASE64` و`SHAREAPK_STORE_PASSWORD` و`SHAREAPK_KEY_PASSWORD`، والاسم المستعار `upload`.
  - `release.yml` ينشر الإصدار عند الدمج في `main`.
- **R8:** مفعّل لنسخة الإصدار. أي مكتبة جديدة تُستخدم بالانعكاس (reflection) تحتاج قاعدة في `app/proguard-rules.pro`.
- **قبل الرفع:** `./gradlew testDebugUnitTest assembleDebug lintDebug`. البناء المحلي يحتاج `debug.keystore` في الجذر (يُولَّد بـ`keytool`، وهو مستثنى من git).
- **حالة العمل والخطط:** في `docs/PROGRESS.md` و`docs/superpowers/`.

## Working preferences
- الرد دائماً بالعربية، مختصراً وعملياً، بما في ذلك أسئلة أداة الاختيار وخياراتها.
- قبل أي ميزة جديدة: ناقش المستخدم واسأله بأسئلة مرقّمة بخيارات مع تمييز الخيار المقترح، وانتظر جوابه. ما يتركه بلا جواب يُقرَّر فيه بالخيار المقترح.
- قلّل استهلاك الرصيد: قسّم العمل إلى مراحل، وسجّل التقدم في `docs/PROGRESS.md` ليُستكمل لاحقاً.
- بعد كل مرحلة: نسخة APK للتجربة على الهاتف.
