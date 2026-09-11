# Launcher3QuickStep Gradle 工程（AOSP 17 / fde_17 移植）

把 `D:\code\packages_apps_launcher3`（纯 Soong 工程）改造成可在 Windows + Android Studio/Gradle 下
直接编译的工程。AOSP 源码目录保持不动，所有 Gradle 相关文件都在本目录（`gradle-modules/`）和
仓库根目录的几个配置文件中。

## 快速开始

```powershell
cd D:\code\packages_apps_launcher3

# 编译 debug APK
.\gradlew.bat :app:assembleDebug --no-parallel

# 产物
#   gradle-modules\app\build\outputs\apk\debug\app-debug.apk        （debug 签名）
#   gradle-modules\app\build\outputs\apk\debug\app-testkey-signed.apk（ROM 用的 testkey 签名，可覆盖安装）

# 平台签名（FDE ROM 用 testkey；换 ROM 时可用 -KeyName platform）
.\gradle-modules\tools\sign_platform.ps1

# 安装到设备（同包名 + 同签名 + versionCode 对齐后可覆盖系统 Launcher）
adb install -r .\gradle-modules\app\build\outputs\apk\debug\app-testkey-signed.apk
```

> **首次克隆必读**：`prebuilts/`（约 200MB 的 ROM 依赖快照，含 framework.jar、SystemUI AAR、
> aconfig flag jar、平台签名 key）已随仓库提交，无需访问 ROM 机器。
> 只需先执行一次本机 SDK 补丁，再编译：
>
> ```powershell
> python .\gradle-modules\tools\merge_android_jar.py
> .\gradlew.bat :app:assembleDebug --no-parallel
> ```
>
> `merge_android_jar.py` 会把 ROM 的 framework 类和资源合并进本机 SDK 的
> `platforms/android-37.0/android.jar`（原文件备份为 `android.jar.gradle-backup`），
> 编译隐藏 API 必需；换机器/换 SDK 时都要执行一次。
> ROM 重新编译后，运行 `sync_from_rom.ps1` 刷新 `prebuilts/`（需要 ROM 机器访问权限）。

环境要求：

| 项目 | 要求 |
|---|---|
| JDK | 21（已在 `gradle.properties` 固定为 `D:/huyang/.gradle/jdks/eclipse_adoptium-21-amd64-windows.2`） |
| Gradle | 8.13（wrapper 已生成，本地已缓存） |
| AGP / Kotlin | 8.13.2 / 2.2.10 |
| Android SDK | `local.properties` 指向 `D:/huyang/Android/sdk`，compileSdk 37 |
| Python | 仅同步脚本需要（`C:\Python314\python.exe`） |

## 工程结构

```
D:\code\packages_apps_launcher3\
├─ settings.gradle.kts          # 包含 :app 与 :widgetpicker
├─ build.gradle.kts             # 插件版本
├─ gradle.properties            # JDK21、AGP/Kotlin 配置、传递 R 等
├─ gradlew / gradlew.bat / gradle/
├─ gradle-modules\              # 本目录（已随仓库提交；prebuilts 一并提交）
│  ├─ app\                      # 主 app 模块（Launcher3QuickStep，namespace com.android.launcher3）
│  ├─ widgetpicker\             # widgetpicker 独立库模块（独立 R 命名空间，必须独立模块）
│  ├─ generated\                # BuildConfig / protolog 转换源码 / 打过补丁的 quickstep-res
│  ├─ prebuilts\                # 从 ROM out/ 抽取的依赖
│  │  ├─ jars\                  # SystemUI 等内部库、androidx 非 AAR 部分、stubs、kotlin、flag 等
│  │  ├─ aars\                  # androidx AAR（原版）+ 自建 SystemUI AAR + dynamiccolors/lottie
│  │  ├─ platform\              # framework.jar、framework-jarjar-turbine.jar、framework-res.apk
│  │  ├─ generated\             # BuildConfig.java、protolog/proto srcjar、aconfig 源码
│  │  ├─ manifest\              # Soong 合并后的 AndroidManifest.xml（manifest_merger）
│  │  └─ keys\                  # testkey / platform 的 pk8 + x509.pem（签名）
│  ├─ rom.properties            # ROM 主机连接配置（已提交，换人用时修改）
│  └─ tools\                    # 抽取/补丁/同步/签名脚本
└─ 其余为 AOSP 原始目录（src、quickstep、res、modules、shared、dagger、src_plugins…）
```

## 依赖引入方式（核心思路）

1. **平台隐藏 API**：Soong 编译 Launcher3QuickStep 用的是平台私有 API。
   - 把 ROM 的 `framework-jarjar-turbine.jar`（hidden aconfig 类已重定位的 **ABI 版**）
     + `framework-res.apk` 的资源表合并进 SDK 的 `platforms/android-37.0/android.jar`
     （原文件备份为 `android.jar.gradle-backup`，脚本 `tools/merge_android_jar.py`）。
   - 注意**不能**把完整 `framework.jar`（含真实方法体）合并进去：Android Studio 的
     MockableJarTransform 会用 ASM `COMPUTE_FRAMES` 重写 android.jar 里的每个类，遇到部分
     真实 framework 字节码会抛 `NullPointerException`；turbine ABI 只含签名，且与 Soong
     编译 Launcher3 时使用的输入一致。
   - 这样 javac/KSP/aapt2 才能看到 `com.android.internal.*`、`android.companion.Flags`、
     `android.widget.TextClock$ClockEventDelegate`、私有 framework 资源等。
2. **AOSP 内部静态库**（SystemUI shared、WM Shell shared、iconloader、mechanics、msdl、
   dynamiccolors 等）Maven 没有，全部从 ROM 的 `out/soong/.intermediates` 抽取：
   - 带资源的库 → 打成 AAR（classes + res + R.txt + manifest），由 `tools/extract_prebuilts.sh`
     和 `tools/build_extra_aars.sh` 在远端生成；
   - 不带资源的库 → 合并 module 下所有 jar 成一个 jar；
   - androidx → 直接取 AOSP 树里 `prebuilts/sdk/current/androidx` 的原始 AAR（版本与 ROM 完全一致）。
3. **构建期生成物**：BuildConfig、ProtoLog 转换后的 srcjar、proto lite 源码、aconfig flag 类，
   都从 ROM 的 `out/soong` 抽取（`prebuilts/generated` 与 `prebuilts/jars`）。
   - `com_android_launcher3_flags_lib.jar` 等 flag 库直接用编译好的 jar。
4. **Dagger**：KSP + `com.google.dagger:dagger-compiler:2.60.1`（Maven 缓存）。
   - 注意：不能用 `kapt`，Maven Kotlin 工具链会在 `SystemUiProxy.kt` 上触发编译器 IR 崩溃；
     也不能同时保留 `annotationProcessor`，否则与 KSP 重复生成 Dagger 类。

## 资源处理

- `res/`（Launcher3ResLib）+ `quickstep/res`（QuickstepResLib，覆盖优先级更高）。
- AGP 不支持同一模块内的资源覆盖，因此 `quickstep/res` 放在 `debug`/`release` source set 作为 overlay。
- `quickstep/res/values/colors.xml` 里有 5 处 `@androidprv:` 引用、约 50 处 `android:featureFlag`，
  AGP 的资源合并器会丢掉 `androidprv` 命名空间且 aapt2 不认识 aconfig flag。
  用 `tools/sync_res.py` 生成打过补丁的副本 `generated/quickstep-res`（`@androidprv:` → `@*android:`，
  去掉 featureFlag 属性），source set 指向该副本。
- `gradle.properties` 中 `android.nonTransitiveRClass=false`（传递 R），
  这样 `R.color.materialColorPrimary` 等由 dynamiccolors 合并进 app 包的资源才能解析（与 Soong 行为一致）。

## 从 ROM 重新同步依赖

ROM 重新编译后（或换了构建机器），运行：

```powershell
# ROM 连接信息按优先级取：命令行参数 > 环境变量 > gradle-modules/rom.properties（本地 git-ignored）
$env:ROM_PASS = "密码"      # 无 SSH key 时使用
.\gradle-modules\tools\sync_from_rom.ps1
```

脚本会：上传抽取脚本 → 远端运行（抽取 + 自建 AAR + flag jar）→ 打包下载 → 替换 `prebuilts/` →
运行 `postprocess_prebuilts.py`（AAR 元数据/资源补丁、剔除重复类）→ `sync_res.py` →
`merge_android_jar.py` → `make_appwidget_stub.ps1`。

同步后必须重新编译（`--no-parallel`，避免 Windows 下 AGP 并行任务的文件占用问题）。

## 依赖来源与上游（AOSP/ROM）变更处理

### 依赖分类与来源

| 类别 | AOSP 来源 | 抽取/引入方式 | 刷新方式 |
|---|---|---|---|
| 平台隐藏 API | `out/.../framework-minus-apex/.../framework.jar`、`.../jarjar/turbine/framework.jar`、`framework-res.apk` | `prebuilts/platform/`，再由 `merge_android_jar.py` 合并进 SDK android.jar | `sync_from_rom.ps1` |
| SystemUI/WM Shell 等内部库（带资源） | `out/soong/.intermediates/frameworks/**`（模块有非空 R.txt） | `extract_prebuilts.sh` 打成 `prebuilts/aars/systemui_*.aar` | 同上 |
| 内部库（无资源） | 同上（R.txt 为空） | `extract_prebuilts.sh` 合并为 `prebuilts/jars/*.jar` | 同上 |
| 仅资源、不在编译 classpath 的库（dynamiccolors、lottie） | 同上 | `build_extra_aars.sh`（**模块列表硬编码**） | 新增此类库要改脚本 |
| androidx / compose / material | `prebuilts/sdk/current/androidx/m2repository` 的原始 AAR | `extract_prebuilts.sh` 直接拷贝 | 同上 |
| aconfig flag 类（编译期） | `frameworks/base/*aconfig-java*` 模块 | 编译 classpath 里的 flag jar | 同上 |
| aconfig flag 类（debug 运行时缺失） | `frameworks/base/android.{companion,os,security,multiuser}.*-aconfig-java` | `collect_flag_jars.sh`（**模块列表硬编码**） | 新增引用要改脚本 |
| 构建期生成物 | `out/soong/.intermediates/packages/apps/Launcher3/**`（BuildConfig/protolog/proto/aconfig/manifest） | `extract_prebuilts.sh` | 同上 |
| 签名 key | `build/make/target/product/security/{testkey,platform}.*` | `extract_prebuilts.sh` | 同上 |

### 先判断变更类型，再决定动作

| 变更 | 需要做什么 |
|---|---|
| 只改 Launcher3 自己的代码（`src/`、`quickstep/src/`、`res/`、`modules/`、`shared/`） | 直接 `gradlew :app:assembleDebug`，**不需要同步**。若 `quickstep/res` 新增了 `@androidprv:` 或 `android:featureFlag`，先跑 `python tools/sync_res.py` 重新生成补丁副本 |
| 改 AOSP 依赖库源码（`frameworks/libs/systemui/**`、`frameworks/base/packages/SystemUI/**`、`frameworks/base/libs/WindowManager/**`、`frameworks/base/core/**`、`prebuilts/sdk/current/androidx/**` 等） | 在 ROM 机器上重新编译（至少 `m Launcher3QuickStep`），然后 `sync_from_rom.ps1` |
| 依赖库 API 变了（类/方法签名） | 同上；Gradle 编译会报错，按错误补依赖或改 Launcher3 代码 |
| `Android.bp` 新增/删除依赖模块 | 同步后确认 `prebuilts/jars`、`prebuilts/aars` 里出现/移除了对应产物；**仅资源、不在编译 classpath 的新库要手动加进 `build_extra_aars.sh`**；新增隐藏 flag 引用要加进 `collect_flag_jars.sh` |
| ROM 升级 JDK/Kotlin/dagger 等工具链 | 对齐本工程版本（见环境要求表）；Kotlin 尤其敏感，2.2.10 是唯一验证可用的 Maven 版本 |
| 换签名 key（如 testkey → releasekey） | `sign_platform.ps1 -KeyName <name>`，并确认 `prebuilts/keys/` 有对应 key（`extract_prebuilts.sh` 目前只拷 testkey/platform） |
| 升级 `compileSdk`/AGP | 见已知问题表；AGP 8.13 最高支持 API 36.1，升 SDK 需要同步升 AGP（AGP 9.x 有 DSL 破坏性变更） |

### 同步后验证清单

1. `git diff --stat gradle-modules/prebuilts` 确认变更范围符合预期
2. `python tools/postprocess_prebuilts.py`（sync 已自动执行）无报错
3. `.\gradlew.bat :app:assembleDebug --no-parallel` 编译通过
4. `sign_platform.ps1` + `adb install -r`，确认 `topResumedActivity=...QuickstepLauncher` 且 `logcat -b crash` 为空

### 参考快照

`tools/overlay.list`（app 资源合并的完整模块列表）与 `tools/cp_kotlin.txt`（首次抽取时的编译 classpath）
是首次移植时的快照，仅用于人工排查"某个资源/类来自哪个模块"，脚本不会自动更新；
同步后可对照 ROM 里的
`out/soong/.intermediates/packages/apps/Launcher3/Launcher3QuickStep/android_common/aapt2/overlay.list`
和 `.../kotlinc/classpath.rsp` 查看差异。

## 已知问题与规避

| 问题 | 原因 | 规避 |
|---|---|---|
| `compileDebugKotlin` IR 崩溃（fake override / lowering） | Maven Kotlin 2.2.0/2.2.21 与 AOSP 定制分支（2.2.0-custom-branch-22）差异 | 固定 Kotlin **2.2.10**（已验证可用） |
| kapt 崩溃 | 同上，kapt stub 生成走 IR 管线 | Dagger 全部改用 **KSP** |
| `FilerException: Attempt to recreate a file` | KSP 与 javac annotationProcessor 同时生成 Dagger 类 | 不声明 `annotationProcessor`，只用 KSP |
| KSP StackOverflowError | `androidx.appfunctions:appfunctions-compiler` 处理器与 Maven Kotlin/KSP 组合 | 已禁用该 KSP 处理器（workspace-functions 的 AppFunction 元数据不生成，不影响启动器主功能） |
| aapt2 找不到私有 framework 资源 / `androidprv` | AGP 资源合并丢命名空间 + SDK android.jar 只有公开资源 | 合并版 android.jar + `@*android:` 补丁 |
| D8 `An API level of 37 is not supported` | AGP 8.13 的 D8 最高支持 API 36 | 仅警告，可忽略；后续可升级 AGP 9.x |
| `bundleLibCompileToJarDebug` 文件占用 | Windows 并行构建文件锁 | 构建时加 `--no-parallel`（`gradle.properties` 已设 `org.gradle.parallel=false`） |
| `forceCompileSdkPreview=Baklava` 检查失败 | AOSP AAR 用预览 SDK 构建 | `postprocess_prebuilts.py` 移除该元数据 |
| 重复类（kotlin-stdlib / annotations / proto 扩展） | AOSP 内部 jar 打包了 stdlib 或覆盖版本 | `postprocess_prebuilts.py` 自动剔除 |
| 运行时 `NoClassDefFoundError: android.companion.Flags` 等 | 这些 aconfig flag 类是 `hidden_from_bootclasspath`，设备 bootclasspath 没有；ROM release 版靠 R8 内联，debug 版必须自带 | `collect_flag_jars.sh` 收集真实 flag jar 打进 APK；`make_appwidget_stub.ps1` 生成 appwidget stub |
| HOME 图标不出现 / `FallbackHome` | 抽到了 `manifest_fixer`（只有 6 个组件），正确的合并 manifest 是 `manifest_merger`（含 `QuickstepLauncher` + HOME） | `extract_prebuilts.sh` 已改为取 `manifest_merger/AndroidManifest.xml` |
| 安装报 `INSTALL_FAILED_VERSION_DOWNGRADE` | 设备上的 Launcher versionCode=37 | `defaultConfig` 已对齐 `versionCode 37 / versionName "17"` |
| 安装报 `signatures do not match` | ROM 用的是 AOSP `testkey` 而不是 platform key | `prebuilts/keys/testkey.*` + `sign_platform.ps1` 默认用 testkey |
| Android Studio 同步报 `Unsupported class file major version 69` | Studio 自带 JBR 25（Java 25 = major 69），Gradle 8.13 的 Groovy 不支持在 JDK 25 上运行 | `Settings → Build Tools → Gradle → Gradle JDK` 改为 **JDK 21**（Add JDK 指向 Temurin 21）；同时确认 `gradle.properties` 里 `org.gradle.java.home` 路径在本机存在 |
| Android Studio 同步报 `MockableJarTransform ... NullPointerException` | android.jar 里合并了含真实方法体的 framework.jar，AGP 的 mockable 转换用 ASM `COMPUTE_FRAMES` 重写时崩溃（`handlerRangeBlock is null`） | `merge_android_jar.py` 已改为只用 turbine ABI 类 + framework-res 资源，重新执行一次该脚本即可 |

## 设备部署（已验证）

```powershell
# 1. 编译
.\gradlew.bat :app:assembleDebug --no-parallel
# 2. testkey 签名（FDE ROM 的签名 key）
.\gradle-modules\tools\sign_platform.ps1
# 3. 覆盖安装（同包名/同签名/versionCode 37）
adb install -r .\gradle-modules\app\build\outputs\apk\debug\app-testkey-signed.apk
# 4. 验证
adb shell dumpsys activity activities | Select-String topResumedActivity
#   -> com.android.launcher3/.uioverrides.QuickstepLauncher
```

设备上当前 HOME Activity 是 `com.android.launcher3.uioverrides.QuickstepLauncher`
（不是 `com.android.launcher3.Launcher`）。

## 运行时依赖说明（debug 与 release 的差异）

ROM 的 release 构建（R8）会把只读 aconfig flag 常量内联，所以 `framework.jar` 里没有
`android.companion.Flags`、`android.os.Flags`、`android.security.Flags`、
`android.multiuser.Flags`、`android.appwidget.flags.Flags` 这些类也能跑。
本工程的 debug APK 不做混淆，必须在 APK 内自带这些类：

- `collect_flag_jars.sh` 从 ROM 的 `*-aconfig-java` 模块收集真实实现打进 APK；
- `android.appwidget.flags.Flags` 没有真实 jar，由 `make_appwidget_stub.ps1` 生成 stub
  （`engagementMetrics()` / `generatedPreviews()` 返回 true）；
- `android.window.DesktopExperienceFlags` / `DesktopModeFlags` 在设备 boot image 中存在，
  无需额外处理。
