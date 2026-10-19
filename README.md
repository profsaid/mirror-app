# Mirror — 3-angle hairstyle try-on (iOS)

SwiftUI app, iOS 17+, iPhone only. Follows Apple's Human Interface Guidelines:
system fonts and colors, SF Symbols, native tab bar, segmented controls, share sheet, Dark Mode.

## تشغيل المشروع

1. فك الضغط وافتح `Mirror.xcodeproj` (محتاج Xcode 16 أو أحدث).
2. من القائمة على الشمال اختار مشروع **Mirror** ← target **Mirror** ← تبويب **Signing & Capabilities**، وفي خانة **Team** اختار الـ Apple ID بتاعك (حساب مجاني يكفي للتجربة على موبايلك).
3. وصّل الآيفون بالكابل واختاره من فوق، ودوس ▶︎ (Cmd+R).
4. أول مرة بس على الآيفون: Settings ← General ← VPN & Device Management ← وثّق حساب المطور، وفعّل **Developer Mode** لو طلب منك.

**على الـ Simulator:** مفيش كاميرا، فالتطبيق هيظهرلك زرار "Choose photo" عشان تختار صورة لكل زاوية من الـ Photos.

## اللي شغال دلوقتي

- **Guided capture:** الكاميرا الأمامية مع Vision بيقيس لفة الراس (yaw). أول ما تثبت في الزاوية الصح حوالي 0.8 ثانية، الصورة بتتاخد لوحدها.
- **مكتبة القصات:** فيها بحث وفلاتر حسب الطول.
- **شاشة النتيجة:** الـ ٣ زوايا مع Before/After وملاحظات للحلاق، وحفظ ومشاركة.
- **الحفظ:** كل الصور بتتحفظ على الآيفون بس، في Documents/Mirror.

## الـ AI

طول ما `Config.tryOnEndpoint` قيمته `nil`، التطبيق بيستخدم `MockTryOnService`، اللي بيرجع صورك ومكتوب عليها "Preview" عشان تجرب الـ flow كله.

الخطوة الجاية: backend صغير (Cloudflare Worker مثلاً) شايل مفتاح الـ AI. بيستقبل الـ ٣ صور مع بعض ويرجعهم بالقصة الجديدة. شكل الطلب والرد موجود في `Services/TryOnService.swift` (`RemoteTryOnService`)، والـ prompt في `TryOnPrompt`.

## ضبط الكاميرا (`App/Config.swift`)

| الإعداد | الوظيفة |
|---|---|
| `yawSign` | لو التطبيق قالك تلف في الاتجاه الغلط، غيّره لـ `-1` |
| `sideYawThreshold` | زاوية اللفة المطلوبة لصور الجنب (35°) |
| `frontYawTolerance` | قد إيه الصورة الأمامية لازم تكون مستقيمة (12°) |
| `holdStillSeconds` | مدة الثبات قبل ما الصورة تتاخد لوحدها |

## هيكل المشروع

```
Mirror/
  App/         MirrorApp, Config
  Models/      HeadAngle, Hairstyle, catalog, AppModel (state + storage)
  Services/    TryOnService (mock + remote)
  Camera/      CameraController (AVFoundation + Vision), CameraPreview
  Views/       Home, Capture, Styles, Result, Saved, shared components
```

الفولدر `Mirror/` متزامن مع Xcode، يعني أي ملف Swift تضيفه جواه بيدخل في الـ build لوحده.
