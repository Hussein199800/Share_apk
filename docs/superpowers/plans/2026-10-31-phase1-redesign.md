# المرحلة 1: إعادة التصميم — خطة التنفيذ

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**الهدف:** واجهة جديدة كاملة بهوية «طاقتي» الملوّنة، وأربعة تبويبات (التطبيقات، إرسال، استقبال، المزيد)، دون أي تغيير في منطق الاستخراج أو التثبيت أو النسخ الاحتياطي.

**البنية:**
- تُبنى طبقة تصميم جديدة: ألوان، وأبعاد، وتدرجات، وخط، ومكوّنات مشتركة.
- يُستبدل `MainNavTab`، وتُضاف حالة `MoreDestination` في الـ ViewModel.
- تُعاد كتابة كل شاشة فوق المكوّنات الجديدة مع الحفاظ على تواقيع الـ callbacks الحالية.
- أسماء الألوان القديمة تبقى مؤقتاً كأسماء بديلة تشير إلى الرموز الجديدة، حتى يبقى المشروع قابلاً للبناء بعد كل مهمة، ثم تُحذف في المهمة الأخيرة.

**التقنيات:** Kotlin 2.2.10، Jetpack Compose (BOM 2024.09.00)، Material3، DataStore، Room، JUnit4، Robolectric 4.16.1، Gradle 9.3.1، AGP 9.1.1.

**المواصفات:** `docs/superpowers/specs/2026-10-31-phase1-redesign-design.md`. والنماذج المعتمدة: https://claude.ai/artifact/3bgngzqJJVkcKGh7Kf7vjN

## القيود العامة
- `minSdk = 31` و`targetSdk = 36` لا يتغيّران.
- لا تُضاف مكتبات تنقّل أو تصميم جديدة. المسموح: ملفات خط IBM Plex Sans Arabic (OFL) فقط.
- المنطق لا يُمس: `ShareApkViewModel` تُضاف إليه حالة التنقل فقط، و`data/`، و`installer/`، و`backup/`، و`service/`، و`wifi/` لا تتغيّر، باستثناء القيمة الافتراضية لـ`ThemePreference`.
- كل نص جديد يُضاف إلى `AppStrings` في `ArabicStrings` و`EnglishStrings` معاً، ولا تُكتب نصوص مباشرة في الواجهة.
- قيم الألوان والتدرجات والأبعاد تُنسخ حرفياً من المواصفات (§3)، ولا يُكتب لون hex خارج `ui/theme/`.
- منطقة اللمس 44dp على الأقل، والأزرار بارتفاع 48dp على الأقل.
- الأرقام وأسماء الحزم والإصدارات تُعرض داخل `LtrBox` (موجود في `Strings.kt`).
- ملف الواجهة الواحد دون 400 سطر تقريباً.
- رسائل الـ commit تنتهي بسطري الإسناد المحددين للجلسة.

## نقاط التركيز في المراجعة
1. **اللغة الإنجليزية (LTR):** يجب أن ينعكس التخطيط كاملاً: شريط التحديد، وترتيب التبويبات، وأسهم «›». يُختبر في المهمة 4.
2. **الأسماء الطويلة وتكبير الخط 1.3×:** أسماء التطبيقات تُقصّ بـ `…` في سطر واحد، والبلاطات لا تتجاوز حدودها. يُختبر في المهمتين 3 و5.
3. **الحالات الفارغة:**
   - بحث لا يطابق شيئاً → رسالة «لا توجد نتائج».
   - سجل فارغ → حالة فارغة.
   - تبويب الإرسال بلا تحديد → دعوة لاختيار تطبيقات.
   - يُختبر في المهام 4 و5 و7.
4. **زر الرجوع:** يتصرف بالترتيب التالي: يلغي التحديد أولاً، ثم يعود من الشاشة الفرعية إلى «المزيد»، ثم ينتقل إلى تبويب التطبيقات، ثم يخرج من التطبيق. يُختبر في المهمة 4.
5. **مستخدم قديم فعّل الألوان الديناميكية أو حفظ `DARK`:** لا يحدث تعطّل، والقيمة المحفوظة تُحترم، ومفتاح الألوان الديناميكية يُتجاهل. يُختبر في المهمة 2.

---

## خريطة الملفات

| الملف | المسؤولية |
|---|---|
| `gradlew`، `gradlew.bat`، `gradle/wrapper/gradle-wrapper.jar` | ملفات Gradle Wrapper المفقودة حالياً |
| `.github/workflows/android.yml` | بناء نسخة Debug، وتشغيل الاختبارات، ورفع ملف APK |
| `ui/theme/Color.kt` | رموز الألوان (`ShareApkColors`) ومخططا Material للوضعين الفاتح والداكن |
| `ui/theme/Gradients.kt` | `ShareApkGradients` و`GradientPair` |
| `ui/theme/ShareApkTokens.kt` | الزوايا والهوامش والأحجام والحركة |
| `ui/theme/Type.kt` | خط `PlexArabic` والمقاسات |
| `ui/theme/Theme.kt` | `ShareAPKTheme(darkTheme)` و`LocalShareApkColors` |
| `res/font/ibm_plex_sans_arabic_{regular,medium,semibold,bold}.ttf` | ملفات الخط |
| `ui/components/design/*.kt` | المكوّنات المشتركة (§3.4)، كل مجموعة في ملف |
| `ui/navigation/NavState.kt` | `MoreDestination` ومنطق الرجوع `resolveBack()` |
| `ui/ShareApkAppRoot.kt` | الجذر: التبويبات، والتوجيه، والنوافذ (نُقل من `MainActivity`) |
| `ui/screens/apps/AppsScreen.kt`، `AppListItem.kt`، `SelectionBar.kt` | شاشة التطبيقات |
| `ui/screens/send/SendScreen.kt`، `ui/screens/receive/ReceiveScreen.kt` | الواجهتان المبدئيتان |
| `ui/screens/more/MoreScreen.kt` | المزيد |
| `ui/screens/backup/BackupCreateContent.kt`، `BackupRestoreContent.kt`، `BackupScreen.kt` | تقسيم `BackupRestoreScreen.kt` |
| `ui/screens/history/HistoryScreen.kt`، `ui/screens/settings/SettingsScreen.kt`، `AboutScreen.kt` | تقسيم `HistoryAndSettingsScreens.kt` |
| `ui/components/sheets/*.kt` | تقسيم `InstallerAndWifiDialogs.kt` |
| `ui/components/AppDetailPanel.kt` | إعادة صياغة |

---

### المهمة 1: أساس البناء (Wrapper و CI) والتحقق من سلامة الكود الحالي

**الملفات:**
- إنشاء: `gradlew`، و`gradlew.bat`، و`gradle/wrapper/gradle-wrapper.jar`، و`.github/workflows/android.yml`
- تعديل: لا شيء، إلا أخطاء بناء موجودة مسبقاً إن ظهرت. كل إصلاح منها يكون في commit مستقل يذكر الخطأ.

**الواجهات:**
- تنتج: الأمرين `./gradlew testDebugUnitTest` و`./gradlew assembleDebug`، وهما يعملان محلياً أو في CI. المهام اللاحقة تستخدمهما للتحقق.

- [ ] **الخطوة 1:** توليد الـ wrapper بإصدار 9.3.1 (المحدد في `gradle-wrapper.properties`). استخدم `gradle wrapper --gradle-version 9.3.1` إذا كان Gradle متاحاً، وإلا فانسخ الملفات القياسية للإصدار نفسه.
- [ ] **الخطوة 2:** تجهيز Android SDK محلياً إن أمكن:
  - يحتاج إلى `cmdline-tools` و`platforms;android-36` و`build-tools`، مع ضبط `ANDROID_HOME`.
  - إذا منعت الشبكة ذلك، فالتحقق كله يكون عبر CI. يُذكر ذلك في التقرير ولا يُعتبر فشلاً.
- [ ] **الخطوة 3:** كتابة `.github/workflows/android.yml`:
  - يعمل عند `push` و`pull_request` و`workflow_dispatch`.
  - على `ubuntu-latest`: `actions/checkout@v4`، ثم `actions/setup-java@v4` (temurin 21)، ثم `gradle/actions/setup-gradle@v4`.
  - ينفّذ `./gradlew testDebugUnitTest assembleDebug`.
  - يرفع `app/build/outputs/apk/debug/*.apk` باسم `shareapk-debug` عبر `actions/upload-artifact@v4`.
- [ ] **الخطوة 4:** التشغيل والتحقق:
  - نفّذ `./gradlew testDebugUnitTest assembleDebug`، محلياً أو بالدفع ومتابعة التشغيل بأدوات `mcp__github__actions_*`.
  - **المتوقع:** `BUILD SUCCESSFUL`، والاختباران الحاليان ناجحان.
  - إذا فشل البناء بسبب كود موجود: صحّح الحد الأدنى فقط، وسجّل كل خطأ في رسالة الـ commit.
- [ ] **الخطوة 5:** Commit بالرسالة `build: add gradle wrapper and CI workflow`.

---

### المهمة 2: رموز التصميم (الألوان، والتدرجات، والأبعاد، والخط، والسمة)

**الملفات:**
- إعادة كتابة: `ui/theme/Color.kt` و`ui/theme/Type.kt` و`ui/theme/Theme.kt`
- إنشاء: `ui/theme/Gradients.kt`، و`ui/theme/ShareApkTokens.kt`، و`res/font/*.ttf`
- تعديل: `data/preferences/UserPreferencesRepository.kt:47` (القيمة الافتراضية `DARK` تصبح `SYSTEM`)، و`MainActivity.kt` (تمرير `darkTheme` فقط)
- اختبار: `app/src/test/java/com/example/ui/theme/ContrastTest.kt`، و`app/src/test/java/com/example/data/ThemeDefaultTest.kt`

**الواجهات:**
- تنتج:
  - `data class ShareApkColors(background, surface, surfaceElevated, input, outline, onBackground, onSurfaceVariant, primary, primaryText, success, warning, info, error: Color, haloA, haloB: Color)`.
  - `val DarkShareApkColors` و`val LightShareApkColors` بقيم المواصفات §3.1.
  - `haloA` و`haloB`: في الداكن `#6366F1@24%` و`#38BDF8@12%`، وفي الفاتح `#6366F1@16%` و`#38BDF8@12%`.
  - `val LocalShareApkColors = staticCompositionLocalOf { DarkShareApkColors }`، و`object ShareApkTheme { val colors: ShareApkColors @Composable get() }`.
  - `data class GradientPair(val start: Color, val end: Color) { fun brush(angleDeg: Float = 145f): Brush }`.
  - `object ShareApkGradients { primary, hero (3 ألوان → Brush), green, sky, amber, violet, indigo, rose: GradientPair; progress: GradientPair }` بقيم §3.1.
  - `object ShareApkTokens { CardRadius = 22.dp; HeroRadius = 24.dp; SmallRadius = 14.dp; ButtonRadius = 16.dp; SheetRadius = 28.dp; Hairline = 0.5.dp; ScreenPadding = 20.dp; CardGap = 10.dp; SectionGap = 16.dp; MinTouch = 44.dp; ButtonHeight = 48.dp; SheetButtonHeight = 52.dp; MotionMs = 200 }`.
  - `val PlexArabic: FontFamily`، و`Typography` تُضبط فيها: `headlineMedium` = 26sp/700، و`titleMedium` = 16sp/600، و`bodyLarge` = 15sp/600 للعناصر، و`bodySmall` = 12.5sp، و`labelSmall` = 11sp/600.
  - `fun ShareAPKTheme(darkTheme: Boolean, content: @Composable () -> Unit)`، وتُحذف معاملة `dynamicColor`.
  - `fun contrastRatio(fg: Color, bg: Color): Double` في `Color.kt`، وفق صيغة WCAG للإضاءة النسبية.
  - **أسماء بديلة مؤقتة:** كل اسم لون قديم في `Color.kt` (`BurnishedGold`، و`EmeraldVerified`، و`AmberWarning`، و`RubyError`، و`DeepEspressoText`…) يبقى `val` يشير إلى أقرب رمز جديد، مع `@Deprecated("Phase1: remove in Task 9")`.

- [ ] **الخطوة 1: كتابة الاختبارات الفاشلة**

```kotlin
class ContrastTest {
    private fun assertAA(fg: Color, bg: Color) =
        assertTrue("ratio=${contrastRatio(fg, bg)}", contrastRatio(fg, bg) >= 4.5)

    @Test fun darkTextPairsPassAA() = with(DarkShareApkColors) {
        listOf(onBackground, onSurfaceVariant, primaryText, success, warning, info, error)
            .forEach { assertAA(it, surface); assertAA(it, background) }
    }
    @Test fun lightTextPairsPassAA() = with(LightShareApkColors) {
        listOf(onBackground, onSurfaceVariant, primaryText, success, warning, info, error)
            .forEach { assertAA(it, surface); assertAA(it, background) }
    }
    @Test fun whiteOnPrimaryButtonPassesAA() =
        assertAA(Color.White, ShareApkGradients.primary.end)
}
```

`ThemeDefaultTest` (Robolectric): مستودع تفضيلات جديد يُعيد `ThemePreference.SYSTEM`. وبعد `setThemePreference(DARK)` يُعيد `DARK`. وكتابة `dynamic_color=true` يدوياً في DataStore لا تسبب أي استثناء عند قراءة `preferencesFlow` (نقطة المراجعة 5).

- [ ] **الخطوة 2:** نفّذ `./gradlew testDebugUnitTest --tests '*ContrastTest' --tests '*ThemeDefaultTest'`. **المتوقع:** فشل في الترجمة لأن `contrastRatio` و`DarkShareApkColors` غير معرّفين.
- [ ] **الخطوة 3: التنفيذ**
  - إضافة ملفات الخط (من حزمة IBM Plex الرسمية، OFL) وكتابة الرموز أعلاه.
  - `Theme.kt` يبني `darkColorScheme`/`lightColorScheme` من `ShareApkColors`:
    - `primary` = `primary`، و`onPrimary` = أبيض.
    - `surfaceVariant` = `surfaceElevated`، و`outlineVariant` = `outline`.
    - `error` = `error`.
  - يوفّر `LocalShareApkColors`.
  - إذا فشل `warning` الفاتح (`#B45309`) على `#EEF1F8`، فيُغمَّق إلى أقرب درجة ناجحة، ويُسجَّل التعديل في الـ commit.
- [ ] **الخطوة 4:** نفّذ `./gradlew testDebugUnitTest assembleDebug`. **المتوقع:** كل الاختبارات ناجحة، والبناء ناجح (الأسماء البديلة تُبقي الشاشات القديمة تعمل).
- [ ] **الخطوة 5:** Commit بالرسالة `feat(theme): new ShareAPK design tokens, Plex Arabic font, system theme default`.

---

### المهمة 3: مكتبة المكوّنات المشتركة

**الملفات:**
- إنشاء في `ui/components/design/`:
  - `Surfaces.kt`: `SurfaceCard`، و`HeroCard`، و`GradientTile`، و`GradientIconBox`، و`HaloBackground`.
  - `Buttons.kt`: `PrimaryButton`، و`SecondaryButton`، و`IconActionButton`.
  - `Inputs.kt`: `SearchField`، و`FilterChipRow`.
  - `Indicators.kt`: `StatusBadge`، و`GradientProgressBar`.
  - `Rows.kt`: `SettingsRow`.
  - `Scaffolds.kt`: `ScreenScaffold`، و`ShareApkBottomSheet`.
  - `NavBars.kt`: `ShareApkBottomBar`، و`ShareApkRail`.
- اختبار: `app/src/test/java/com/example/ui/components/design/DesignComponentsTest.kt` (Robolectric + `createComposeRule`)

**الواجهات:**
- تستهلك: رموز المهمة 2.
- تنتج (كل المكوّنات تقبل `modifier: Modifier = Modifier` كآخر معاملة قبل `content`):
  - `SurfaceCard(onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit)`.
  - `HeroCard(onClick: (() -> Unit)? = null, content: @Composable RowScope.() -> Unit)`: تدرج `hero`، ونص أبيض، وزوايا `HeroRadius`.
  - `GradientTile(icon: ImageVector, title: String, subtitle: String, gradient: GradientPair, enabled: Boolean = true, badge: String? = null, onClick: () -> Unit)`: العنوان سطر واحد بـ `…`، والوصف سطران كحد أقصى، والارتفاع الأدنى 126dp.
  - `GradientIconBox(gradient: GradientPair, size: Dp = 46.dp, content: @Composable BoxScope.() -> Unit)`، و`LetterIconBox(label: String, size: Dp = 46.dp)`، الذي يختار التدرج حتمياً بالشكل `label.hashCode().mod(6)` من قائمة [green, sky, amber, violet, indigo, rose].
  - `HaloBackground(content: @Composable () -> Unit)`: تدرجان شعاعيان من `haloA` و`haloB` خلف المحتوى.
  - `PrimaryButton(text: String, onClick: () -> Unit, icon: ImageVector? = null, enabled: Boolean = true, height: Dp = ShareApkTokens.ButtonHeight)`.
  - `SecondaryButton(...)` بنفس التوقيع.
  - `IconActionButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit)`، ومقاسه 44dp.
  - `SearchField(value: String, onValueChange: (String) -> Unit, placeholder: String)`.
  - `FilterChipRow(options: List<Pair<K, String>>, selected: K?, onSelect: (K?) -> Unit)`، وهو عام بنوع `<K>`. أول شريحة دائماً «الكل» وتعني `null`.
  - `enum class BadgeTone { Success, Warning, Info, Error, Primary }`، و`StatusBadge(text: String, tone: BadgeTone, icon: ImageVector? = null)`.
  - `GradientProgressBar(progress: Float?, height: Dp = 8.dp)`، حيث `null` تعني تقدماً غير محدد.
  - `SettingsRow(icon: ImageVector, title: String, subtitle: String? = null, onClick: (() -> Unit)? = null, trailing: @Composable (() -> Unit)? = null)`.
  - `ScreenScaffold(title: String, subtitle: String? = null, action: @Composable (() -> Unit)? = null, onBack: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit)`، مع هامش سفلي يترك مكاناً لشريط التبويب.
  - `ShareApkBottomSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit)`، فوق `ModalBottomSheet` بزوايا `SheetRadius`.
  - `data class NavItem(val tab: MainNavTab, val label: String, val icon: ImageVector)`.
  - `ShareApkBottomBar(items: List<NavItem>, current: MainNavTab, onSelect: (MainNavTab) -> Unit)`: شريط عائم بهامش 12dp وارتفاع 70dp، والعنصر النشط بتدرج `primary`. يستخدم `testTag("nav_${tab.name}")`.
  - `ShareApkRail(...)` بنفس التوقيع.

- [ ] **الخطوة 1: كتابة الاختبارات الفاشلة**

```kotlin
@Test fun bottomBarInvokesSelectWithTab() { /* setContent 4 NavItem; performClick on nav_SEND; assertEquals(MainNavTab.SEND, picked) */ }
@Test fun filterChipAllMapsToNull() { /* click "الكل" → onSelect(null) */ }
@Test fun gradientTileDisabledDoesNotClick() { /* enabled=false → onClick not called; badge text shown */ }
@Test fun letterIconBoxIsDeterministic() { /* same label twice → same gradient index; first char rendered */ }
@Test fun longTitleTileTruncatesAtLargeFont() { /* Density(fontScale=1.3f), 80-char title, width 170dp → node bounds height <= 200dp */ }
```

المهمة 4 ستعيد تسمية `MainNavTab`، لذلك يكتب الاختبار الأول بقيمة `SEND`، وتُضاف القيم الجديدة إلى الـ enum هنا مؤقتاً بجانب القديمة.

- [ ] **الخطوة 2:** نفّذ `./gradlew testDebugUnitTest --tests '*DesignComponentsTest'`. **المتوقع:** فشل في الترجمة.
- [ ] **الخطوة 3:** تنفيذ المكوّنات بالتواقيع أعلاه، وإضافة `SEND` و`RECEIVE` و`MORE` إلى `MainNavTab`.
- [ ] **الخطوة 4:** نفّذ `./gradlew testDebugUnitTest assembleDebug`. **المتوقع:** نجاح.
- [ ] **الخطوة 5:** Commit بالرسالة `feat(ui): shared design components`.

---

### المهمة 4: التنقل الجديد، والجذر، وشاشات المزيد والإرسال والاستقبال

**الملفات:**
- إنشاء: `ui/navigation/NavState.kt`، و`ui/ShareApkAppRoot.kt`، و`ui/screens/more/MoreScreen.kt`، و`ui/screens/send/SendScreen.kt`، و`ui/screens/receive/ReceiveScreen.kt`
- تعديل:
  - `data/model/AppModels.kt:38-43`: `MainNavTab { APPS, SEND, RECEIVE, MORE }` فقط.
  - `ui/viewmodel/ShareApkViewModel.kt:88-89,272-277`: إضافة `moreDestination`، و`openMore`، و`onBack`. استدعاء `refreshCacheSize()` ينتقل إلى `openMore(SETTINGS)`.
  - `MainActivity.kt`: يُحذف `ShareApkAppRoot` والشريطان منه، ويبقى الإعداد والـ Intents ومشغّلات النتائج (activity launchers). المشغّلات تنتقل إلى الجذر كما هي.
  - `ui/theme/Strings.kt`: نصوص التبويبات والمزيد والإرسال والاستقبال المبدئية.
- اختبار: `app/src/test/java/com/example/ui/navigation/NavStateTest.kt`، و`app/src/test/java/com/example/ui/RootNavigationTest.kt`

**الواجهات:**
- تستهلك: `ShareApkBottomBar`، و`ShareApkRail`، و`NavItem`، و`GradientTile`، و`HeroCard`، و`ScreenScaffold` من المهمة 3.
- تنتج:
  - `enum class MoreDestination { HOME, BACKUP, HISTORY, SETTINGS, ABOUT }`.
  - `data class NavSnapshot(val tab: MainNavTab, val more: MoreDestination, val hasSelection: Boolean)`.
  - `sealed interface BackAction { object ClearSelection; object MoreHome; object GoApps; object Exit }`.
  - `fun resolveBack(s: NavSnapshot): BackAction`، وهي دالة صافية.
  - في الـ ViewModel: `val moreDestination: StateFlow<MoreDestination>`، و`fun openMore(dest: MoreDestination)` (تضبط `tab = MORE`)، و`fun onBack(): Boolean` (تُعيد `false` عند `Exit`).
  - `@Composable fun ShareApkAppRoot(viewModel: ShareApkViewModel, isExpandedScreen: Boolean)` في `ui/ShareApkAppRoot.kt`.
  - `MoreScreen(deviceName: String, appVersion: String, onOpen: (MoreDestination) -> Unit, onInstallFromFile: () -> Unit, onShareViaLink: () -> Unit)`، وبلاطاتها وفق جدول المواصفات §5.4. بلاطة التحديثات `enabled = false` مع شارة «قريباً».
  - `SendScreen(selectedApps: List<InstalledAppInfo>, onPickApps: () -> Unit, onShareViaLink: () -> Unit)`.
  - `ReceiveScreen(deviceName: String)`.

- [ ] **الخطوة 1: كتابة الاختبارات الفاشلة**

```kotlin
class NavStateTest {
    @Test fun selectionClearedFirst() = assertEquals(BackAction.ClearSelection,
        resolveBack(NavSnapshot(MainNavTab.MORE, MoreDestination.SETTINGS, hasSelection = true)))
    @Test fun subPageReturnsToMoreHome() = assertEquals(BackAction.MoreHome,
        resolveBack(NavSnapshot(MainNavTab.MORE, MoreDestination.BACKUP, false)))
    @Test fun otherTabGoesToApps() = assertEquals(BackAction.GoApps,
        resolveBack(NavSnapshot(MainNavTab.SEND, MoreDestination.HOME, false)))
    @Test fun appsTabExits() = assertEquals(BackAction.Exit,
        resolveBack(NavSnapshot(MainNavTab.APPS, MoreDestination.HOME, false)))
}
```

`RootNavigationTest` (Robolectric + Compose):
- تظهر التبويبات الأربعة، والضغط على `nav_MORE` يُظهر بلاطة «الإعدادات».
- الضغط على الإعدادات ثم `pressBack` يعيد إلى «المزيد».
- في وضع `LanguagePreference.ENGLISH` يكون `LocalLayoutDirection == Ltr`، و`nav_APPS` أقصى اليسار: `left` أصغر من `left` لـ `nav_MORE` (نقطة المراجعة 1).
- `SendScreen` بقائمة فارغة يعرض نص الدعوة لاختيار تطبيقات (نقطة المراجعة 3).

- [ ] **الخطوة 2:** نفّذ `./gradlew testDebugUnitTest --tests '*NavStateTest' --tests '*RootNavigationTest'`. **المتوقع:** فشل.
- [ ] **الخطوة 3: التنفيذ**
  - الجذر يلفّ المحتوى بـ `HaloBackground`، ويختار `ShareApkBottomBar` أو `ShareApkRail` حسب `isExpandedScreen`.
  - يوجّه المحتوى بالشكل `MORE` + `moreDestination` → `BackupRestoreScreen` / `HistoryScreen` / `SettingsAndAboutScreen` الحالية. هذه الشاشات تُعاد صياغتها في المهام 6–7.
  - `BackHandler(enabled = true) { if (!viewModel.onBack()) activity.finish() }`.
  - كل النوافذ الحالية تنتقل إلى الجذر كما هي.
  - زر «إرسال» في التطبيقات يستدعي `selectTab(SEND)`.
  - `onShareViaLink` في الإرسال يستدعي `startWifiShareForApps(selected)` إذا وُجد تحديد، وإلا `selectTab(APPS)`.
  - اسم الجهاز يُؤخذ من `Build.MODEL`، والإصدار من `BuildConfig.VERSION_NAME` أو `PackageInfo`.
- [ ] **الخطوة 4:** نفّذ `./gradlew testDebugUnitTest assembleDebug`. **المتوقع:** نجاح.
- [ ] **الخطوة 5:** Commit بالرسالة `feat(nav): four-tab navigation, More hub, Send/Receive placeholders`.

---

### المهمة 5: شاشة التطبيقات ولوحة التفاصيل

**الملفات:**
- إنشاء: `ui/screens/apps/AppsScreen.kt`، و`ui/screens/apps/AppListItem.kt` (عنصر القائمة وبطاقة الشبكة)، و`ui/screens/apps/SelectionBar.kt`
- حذف: `ui/screens/AppsHomeScreen.kt` بعد النقل
- إعادة صياغة: `ui/components/AppDetailPanel.kt` بالمحتوى نفسه، وبـ `SurfaceCard` و`StatusBadge` و`PrimaryButton`
- تعديل: `ui/ShareApkAppRoot.kt` (الاستدعاء)
- اختبار: `app/src/test/java/com/example/ui/screens/apps/AppsScreenTest.kt`

**الواجهات:**
- تستهلك: مكوّنات المهمة 3، و`InstalledAppInfo`.
- تنتج: `AppsScreen(...)` بنفس معاملات `AppsHomeScreen` الحالية (`AppsHomeScreen.kt:95-128`)، مع إضافة `onSendSelected: () -> Unit`. الاستدعاء في الجذر لا يتغيّر إلا بإضافة هذه المعاملة.

- [ ] **الخطوة 1: كتابة الاختبارات الفاشلة**
  - `selectionBarAppearsWithSelection`: يظهر الشريط عند `selectedPackageNames = setOf(pkg)`، وفيه نص العدد وزر «إرسال». والضغط عليه يستدعي `onSendSelected`.
  - `selectionBarHiddenWhenEmpty`.
  - `emptySearchShowsNoResults`: عند `apps = emptyList()` و`searchQuery = "zzz"` تظهر رسالة `strings.noResults` (نقطة المراجعة 3).
  - `longAppNameEllipsizes`: اسم من 120 حرفاً يبقى في سطر واحد، و`TextLayoutResult.lineCount == 1` (نقطة المراجعة 2).
  - `splitBadgeShownForSplitApps`.
  - `filterAllClearsFilter`: الضغط على «الكل» يستدعي `onFilterChange(null)`.
    - **قرار:** `AppFilter?` يصبح nullable في `ShareApkViewModel.setFilter`، و`null` تعني بلا فلتر. السبب أن المواصفات تطلب شريحة «الكل».
    - يُتحقق أن منطق التصفية الحالي يعامل `null` كالكل، ويُضاف اختبار وحدة لذلك في `ShareApkViewModel`.
- [ ] **الخطوة 2:** نفّذ `./gradlew testDebugUnitTest --tests '*AppsScreenTest'`. **المتوقع:** فشل.
- [ ] **الخطوة 3: التنفيذ**
  - أيقونة التطبيق الحقيقية تُحمَّل من `PackageManager`، بالطريقة الحالية نفسها، وإن لم تتوفر فـ `LetterIconBox`.
  - شريط التحديد عائم فوق شريط التبويب بمسافة 96dp، وفيه: مشاركة، وحفظ، ورابط واي فاي، وإرسال، بالإضافة إلى إجراء نسخ ZIP السريع الحالي في قائمة «⋯».
  - تقدم ZIP السريع يُعرض بـ `GradientProgressBar`.
- [ ] **الخطوة 4:** نفّذ `./gradlew testDebugUnitTest assembleDebug`. **المتوقع:** نجاح.
- [ ] **الخطوة 5:** Commit بالرسالة `feat(apps): redesigned apps screen, selection bar, detail panel`.

---

### المهمة 6: النسخ الاحتياطي والاستعادة

**الملفات:**
- إنشاء في `ui/screens/backup/`:
  - `BackupScreen.kt`: محوّل بين التدفقين.
  - `BackupCreateContent.kt`: من `FlowACreateBackupContent`.
  - `BackupRestoreContent.kt`: من `FlowBRestoreBackupContent` و`SafetyCheckboxRow`.
- حذف: `ui/screens/BackupRestoreScreen.kt`
- اختبار: `app/src/test/java/com/example/ui/screens/backup/BackupScreenTest.kt`

**الواجهات:**
- تنتج: `BackupScreen(...)` بنفس معاملات `BackupRestoreScreen` الحالية (`BackupRestoreScreen.kt:81-111`)، بالإضافة إلى `onBack: () -> Unit` الذي يمرّر إلى `ScreenScaffold`.

- [ ] **الخطوة 1: كتابة الاختبارات الفاشلة**
  - `startBackupDisabledUntilSafetyChecked`: زر البدء معطّل حتى تُفعَّل الخانات الثلاث، لمطابقة السلوك الحالي.
  - `progressShownWhileRunning`: يظهر `GradientProgressBar` عندما يكون `backupServiceState` في حالة التشغيل.
  - `backButtonInvokesOnBack`.
- [ ] **الخطوة 2:** التشغيل. **المتوقع:** فشل.
- [ ] **الخطوة 3:** نقل المحتوى إلى المكوّنات الجديدة دون تغيير أي شرط أو callback.
- [ ] **الخطوة 4:** نفّذ `./gradlew testDebugUnitTest assembleDebug`. **المتوقع:** نجاح.
- [ ] **الخطوة 5:** Commit بالرسالة `feat(backup): redesigned backup & restore screens`.

---

### المهمة 7: السجل والإعدادات وحول التطبيق

**الملفات:**
- إنشاء: `ui/screens/history/HistoryScreen.kt`، و`ui/screens/settings/SettingsScreen.kt`، و`ui/screens/settings/AboutScreen.kt`
- حذف: `ui/screens/HistoryAndSettingsScreens.kt`، وكذلك `setDynamicColorEnabled` من الـ ViewModel ومن الواجهة. أما المستودع فيبقى كما هو.
- اختبار: `app/src/test/java/com/example/ui/screens/SettingsHistoryTest.kt`

**الواجهات:**
- تنتج:
  - `HistoryScreen(historyItems, onDeleteItem, onClearAll, onBack)`.
  - `SettingsScreen(prefs, cacheSizeBytes, onThemeChange, onLanguageChange, onNamingFormatChange, onClearCacheNow, onAutoClearCacheChange, onBack)`، بدون `onDynamicColorChange`.
  - `AboutScreen(appVersion: String, onBack: () -> Unit)`: الاسم، والإصدار، والوصف، وسطر رخصة الخط «IBM Plex Sans Arabic — SIL Open Font License 1.1».

- [ ] **الخطوة 1: كتابة الاختبارات الفاشلة**
  - `emptyHistoryShowsEmptyState` (نقطة المراجعة 3).
  - `historyItemShowsTypeBadge`.
  - `themeOptionsAreSystemLightDark`: الخيارات الثلاثة موجودة، والضغط على «فاتح» يستدعي `onThemeChange(LIGHT)`.
  - `noDynamicColorOption`: لا يوجد نص `strings.dynamicColor`.
- [ ] **الخطوة 2:** التشغيل. **المتوقع:** فشل.
- [ ] **الخطوة 3:** التنفيذ بـ `SettingsRow` و`SurfaceCard`، وحذف نص `dynamicColor` من `AppStrings` إن لم يعد مستخدماً.
- [ ] **الخطوة 4:** نفّذ `./gradlew testDebugUnitTest assembleDebug`. **المتوقع:** نجاح.
- [ ] **الخطوة 5:** Commit بالرسالة `feat(settings): redesigned history, settings, about`.

---

### المهمة 8: النوافذ السفلية وشاشات الترحيب

**الملفات:**
- إنشاء في `ui/components/sheets/`:
  - `PreInstallSheet.kt`
  - `SplitWarningSheet.kt`
  - `UnknownSourcesSheet.kt`
  - `InstallResultSheet.kt`
  - `WifiShareSheet.kt`
  - `OnboardingPager.kt`
- حذف: `ui/components/InstallerAndWifiDialogs.kt`
- اختبار: `app/src/test/java/com/example/ui/components/sheets/SheetsTest.kt`

**الواجهات:**
- تنتج: نفس أسماء الدوال الحالية ومعاملاتها (`PreInstallValidationSheet`، و`SplitApkWarningDialog`، و`UnknownSourcesPermissionDialog`، و`InstallStatusResultDialog`، و`WifiShareServerDialog`، و`OnboardingPagerDialog`). لذلك لا يتغيّر الجذر إلا في سطور الاستيراد. كلها مبنية على `ShareApkBottomSheet`، عدا الترحيب الذي يبقى بملء الشاشة.

- [ ] **الخطوة 1: كتابة الاختبارات الفاشلة**
  - `wifiSheetShowsUrlAndCountdown`: يظهر `state.url` والوقت المتبقي بصيغة `m:ss`. والضغط على «إيقاف» يستدعي `onStopServer`.
  - `installResultSuccessUsesSuccessTone`.
  - `onboardingLastPageFinishes`: الصفحة الأخيرة تشرح التبويبات الأربعة، والضغط على «ابدأ» يستدعي `onFinishOnboarding`.
- [ ] **الخطوة 2:** التشغيل. **المتوقع:** فشل.
- [ ] **الخطوة 3:** التنفيذ: الأزرار الرئيسية في النوافذ بارتفاع `SheetButtonHeight`، و«رفض/إلغاء» بـ `SecondaryButton`.
- [ ] **الخطوة 4:** نفّذ `./gradlew testDebugUnitTest assembleDebug`. **المتوقع:** نجاح.
- [ ] **الخطوة 5:** Commit بالرسالة `feat(ui): redesigned sheets and onboarding`.

---

### المهمة 9: التنظيف والتحقق النهائي

**الملفات:**
- تعديل: `ui/theme/Color.kt` (حذف الأسماء البديلة `@Deprecated`)، و`ui/theme/Strings.kt` (حذف النصوص غير المستخدمة: `navBackup`، و`navHistory`، و`navSettings`…)، و`res/values*/colors.xml` و`themes.xml` (لون الخلفية لشاشة البداية من `#070D1D` و`#EEF1F8`)
- اختبار: إعادة تشغيل كل الاختبارات

- [ ] **الخطوة 1:** حذف الأسماء البديلة. `grep -rn "Deprecated(\"Phase1" app/src` يجب أن يُرجع لا شيء، ثم البناء.
- [ ] **الخطوة 2:** التحقق من عدم وجود ألوان مباشرة خارج `ui/theme/`. الأمر `grep -rnE "Color\(0x" app/src/main/java --include=*.kt | grep -v ui/theme` يجب أن يُرجع لا شيء.
- [ ] **الخطوة 3:** التحقق من حجم الملفات. الأمر `wc -l app/src/main/java/com/example/ui/**/*.kt` يجب ألا يُظهر ملفاً فوق 450 سطراً تقريباً، باستثناء `Strings.kt` لأنه بيانات.
- [ ] **الخطوة 4:** نفّذ `./gradlew testDebugUnitTest assembleDebug lintDebug`. **المتوقع:** نجاح، وعدم ظهور أخطاء Lint جديدة.
- [ ] **الخطوة 5:** الدفع والتأكد من نجاح CI، ثم رفع رابط ملف APK للمالك مع قائمة التحقق اليدوي من المواصفات §8.
- [ ] **الخطوة 6:** Commit بالرسالة `chore: remove legacy palette aliases and unused strings`.
