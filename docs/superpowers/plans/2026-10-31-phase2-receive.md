# المرحلة 2: الاستقبال المباشر (متوافق مع LocalSend) — المواصفات والخطة

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:executing-plans. Steps use `- [ ]`.

**الهدف:** يستقبل «شارك أبك» التطبيقات من أي جهاز عليه «شارك أبك» أو LocalSend على نفس الشبكة، مع قبول يدوي، ومقارنة الإصدارات، و«ثبّت الآن».

**المرجع:** LocalSend Protocol v2.2 (`github.com/localsend/protocol`). المواصفات العامة: `docs/superpowers/specs/2026-10-31-phase1-redesign-design.md` (الهوية البصرية).

## القرارات المعتمدة (من المالك)
- يكون الجهاز قابلاً للاكتشاف **والتطبيق مفتوح**. أثناء النقل فقط تعمل خدمة أمامية (`dataSync`) تُبقي العملية حية.
- الخادم مبني على **Ktor + Netty** فوق HTTPS، بشهادة ذاتية التوقيع تُولَّد بمكتبة BouncyCastle.
- **القبول يدوي دائماً**، ولا توجد قائمة أجهزة موثوقة.
- **يُقبل فقط:** `apk/apks/xapk/apkm`. القبول الجزئي مسموح كما في البروتوكول. إذا لم يبقَ أي ملف مقبول يُرد بـ`403`.

## البروتوكول (ما نطبّقه)
| العنصر | القيمة |
|---|---|
| الإعلان المتعدد (Multicast) | `224.0.0.167:53317` UDP. نعلن عند التشغيل، ونرد على كل إعلان `announce=true` بطلب `POST /register` إلى المُعلِن، وبإعلان UDP `announce=false` احتياطاً |
| خادم HTTPS | المنفذ `53317`. إذا كان مشغولاً نستخدم منفذاً عشوائياً ونعلن عنه |
| البصمة | SHA-256 لشهادة DER، بصيغة hex صغيرة |
| `POST /api/localsend/v2/register` | يرد بمعلومات جهازنا |
| `GET /api/localsend/v2/info` | يرد بمعلومات جهازنا |
| `POST /api/localsend/v2/prepare-upload` | ينتظر قرار المستخدم (مهلة 60 ث). القبول → `200 {sessionId, files}`، والرفض أو انتهاء المهلة → `403`، ووجود جلسة نشطة → `409` |
| `POST /api/localsend/v2/upload?sessionId&fileId&token` | يتحقق من الجلسة والرمز وعنوان IP (→ `403`)، ثم يكتب البيانات بالتدفق، ويتحقق من sha256 إن أُرسل (→ `422`) |
| `POST /api/localsend/v2/cancel?sessionId` | يلغي الجلسة ويحذف الملفات الجزئية |
| حقل إضافي في وصف الملف | `"shareApk": {packageName, versionName, versionCode}` اختياري. يرسله «شارك أبك» في المرحلة 3، وLocalSend يتجاهله |

قيم جهازنا: `alias` من الإعدادات (افتراضياً `Build.MODEL`)، و`version="2.1"`، و`deviceType="mobile"`، و`protocol="https"`، و`download=false`.

## مقارنة الإصدار
`compareWithInstalled(packageName, versionCode, installed: PackageInfo?)` تعيد إحدى القيم: `NEW` (غير مثبت)، `UPDATE` (أحدث)، `SAME`، `OLDER` (يتطلب حذف النسخة الحالية).
- إذا وصل حقل `shareApk`: تُعرض المقارنة **قبل** القبول.
- إذا لم يصل: تُحسب **بعد** الاستلام، بقراءة الملف عبر `PackageManager.getPackageArchiveInfo`. للحِزم المضغوطة تُقرأ بعد استخراج base.apk.

## الخطة
### المهمة 1: النماذج وJSON
- تفعيل `moshi.kotlin` و`moshi.kotlin.codegen` (KSP) الموجودَين في `libs.versions.toml`.
- إنشاء `receive/protocol/Dtos.kt`: `DeviceInfoDto`، `FileDto(id, fileName, size, fileType, sha256?, preview?, shareApk?)`، `ShareApkMetaDto`، `PrepareUploadRequest(info, files: Map)`، `PrepareUploadResponse(sessionId, files: Map)`.
- إنشاء `receive/protocol/ProtocolJson.kt` (Moshi).
- **اختبار** (JVM): تحليل أمثلة README كما هي، وتجاهل الحقول المجهولة، وقراءة `shareApk`.

### المهمة 2: الهوية والشهادة
- `receive/identity/CertificateStore.kt`: يولّد زوج RSA-2048 وشهادة X.509 صالحة 10 سنوات، ويحفظهما في `filesDir/localsend.p12`، ويعيد `fingerprint`.
- `DeviceIdentity(alias, fingerprint, port)` → `DeviceInfoDto`.
- **اختبار:** الاستدعاء الثاني يعيد نفس البصمة، والبصمة 64 حرف hex.

### المهمة 3: منطق الجلسات (بدون شبكة)
- `receive/ReceiveSessionManager.kt`، وهو قابل للاختبار عبر حقن `clock`، ومجلد الوجهة، ومولّد المعرّفات:
  - `suspend fun prepare(req, senderIp): PrepareResult` (`Accepted(resp)`/`Rejected`/`Blocked`/`NothingToAccept`): يرشّح الامتدادات ويُصدر `IncomingRequest` إلى `StateFlow`، ثم ينتظر `decide(accept: Boolean)` أو انتهاء المهلة.
  - `suspend fun receive(sessionId, fileId, token, senderIp, body: InputStream): UploadResult` (`Ok`/`Forbidden`/`Blocked`/`ChecksumMismatch`/`Error`): يكتب في `received/<sessionId>/<safeName>`، ويحدّث التقدم، ويتحقق من sha256.
  - `fun cancel(sessionId)`. ويُصدر `ReceiveState` (Idle | AwaitingDecision | Receiving(progress) | Done(files) | Failed).
  - تنظيف أسماء الملفات: إزالة المسارات و`..`.
- **اختبارات:** ترشيح الامتدادات (ملف png → `NothingToAccept`، والملفات المختلطة → قبول جزئي)، والرفض، والمهلة، و`409` أثناء جلسة، والرمز الخاطئ أو عنوان IP الخاطئ → `Forbidden`، وعدم تطابق sha256 → حذف الملف و`ChecksumMismatch`، وتنظيف `../evil.apk`، والإلغاء يحذف الملفات الجزئية.

### المهمة 4: خادم HTTPS
- إضافة `ktor-server-core` و`ktor-server-netty` (3.0.3) و`bcpkix-jdk18on` (1.78.1)، واستثناءات `packaging` لملفات `META-INF` المكررة.
- `receive/server/LocalSendServer.kt`: `start(port): Int` و`stop()`، ويربط المسارات بـ`ReceiveSessionManager`.
- **اختبار** (JVM): خادم على منفذ عشوائي، وعميل OkHttp يثق بالشهادة، والتحقق من: `register` و`info` و`prepare` (مع قبول آلي في الاختبار) و`upload` و`cancel` ورموز الأخطاء.

### المهمة 5: الاكتشاف
- `receive/discovery/MulticastResponder.kt`: `buildAnnouncement(identity, announce)` و`parse(bytes)` دالتان صافيتان. الحلقة تعمل على `MulticastSocket` مع `WifiManager.MulticastLock`. تتجاهل بصمتنا، وترد بـ`POST /register` (OkHttp يثق بالشهادة) أو UDP.
- **اختبار:** بناء الرسالة وتحليلها، وتجاهل الذات، والرد فقط على `announce=true`.

### المهمة 6: دورة الحياة والخدمة
- `receive/ReceiveController.kt`، وهو كائن واحد على مستوى التطبيق:
  - `start()` و`stop()`: الخادم والاكتشاف وقفل المتعدد.
  - يتبع `ProcessLifecycleOwner`: يبدأ في المقدمة إذا كان الاستقبال مفعّلاً، ويتوقف في الخلفية ما لم يكن هناك نقل جارٍ.
- `service/ReceiveForegroundService.kt`: نوع `dataSync`، يبدأ مع أول `upload` ويتوقف عند الانتهاء.
- إضافة `lifecycle-process`، والصلاحيات `CHANGE_WIFI_MULTICAST_STATE` و`ACCESS_WIFI_STATE` و`FOREGROUND_SERVICE_DATA_SYNC`.
- التفضيلات: `deviceAlias` و`receiveEnabled` (افتراضياً `true`).
- **اختبار** (Robolectric): القيم الافتراضية للتفضيلات، وانتقالات الحالة في المتحكم مع خادم وهمي.

### المهمة 7: الواجهة
- **`ReceiveScreen`:**
  - بطاقة بارزة فيها الاسم، وعنوان IP:المنفذ، ومفتاح الاستقبال.
  - حالة الانتظار.
  - قائمة «المستلَمة» فيها التطبيق والمقارنة وزر «ثبّت الآن»، الذي يستدعي `inspectAndPrepareInstall(FileProvider uri)`، وزر حذف.
- **`IncomingRequestSheet`:** المرسل، والملفات مع شارات المقارنة، وتحذير لـ`OLDER`، وزرّا رفض وقبول.
- **أثناء النقل:** شريط تقدم وزر إلغاء.
- **الإعدادات:** حقل «اسم الجهاز».
- **السجل:** نوع جديد `RECEIVE`.
- **اختبارات Compose:** الورقة تعرض الشارات، والقبول يستدعي `decide(true)`، والقائمة الفارغة تُظهر الحالة الفارغة، وزر التثبيت يستدعي الإجراء.

### المهمة 8: تثبيت الحِزم المستلمة
- `PackageInstallerManager`: يقبل `xapk/apkm` (أي ملف zip يحتوي ملفات `.apk`)، مع الإبقاء على فحص الأمان ضد اختراق المسارات.
- **اختبار:** zip يحتوي `base.apk` و`config.arm64_v8a.apk` → يُكتشف كحزمة. ومدخل `../x.apk` → يُرفض.

### المهمة 9: التحقق
- الاختبارات والبناء والفحص الآلي (Lint) كلها ناجحة، والتكامل المستمر (CI) أخضر.
- لقطات الشاشات (Robolectric).
- تعليمات اختبار يدوي للمالك: LocalSend على الحاسوب → «شارك أبك».

## نقاط تركيز المراجعة
1. **عميل LocalSend حقيقي:** يتطلب HTTP/1.1 بطول محتوى (Content-Length) كبير يتجاوز 100 م.ب. يجب أن تكون الكتابة بالتدفق دون تحميل الملف كاملاً في الذاكرة.
2. **ملف بنفس الاسم مرتين في نفس الجلسة:** يُضاف رقم للاسم بدل الكتابة فوق الأول.
3. **إغلاق التطبيق أثناء النقل:** تبقيه الخدمة الأمامية حياً، ويستمر النقل.
4. **فقدان الواي فاي أو تغيّر عنوان IP:** يُعاد الإعلان، ولا يحدث انهيار.
5. **تشغيل نسختين من الخادم:** المنفذ 53317 مشغول → يُستخدم منفذ بديل ويُعلَن عنه.
