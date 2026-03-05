# 技術設計書: camera-image-input

## 1. 概要

### 1.1 目的

メイン入力画面（`MainScreen`）にカメラ起動ボタンを追加し、血圧計を撮影した画像から ML Kit Text Recognition v2 を用いて収縮期血圧・拡張期血圧・脈拍の数値を認識し、入力フォームへ自動反映する。

### 1.2 設計方針

- **既存パターン拡張**: 既存の MVVM + Repository パターンを維持・拡張する
- **新規画面なし**: `MainScreen` に機能を追加する形で実装し、既存ナビゲーション構造を変更しない
- **オンデバイス処理**: ML Kit バンドル型を採用し、ネットワーク不要の OCR を実現する
- **フォールバック保証**: カメラ権限拒否・認識失敗時は常に手動入力へ誘導し、既存フローを維持する

### 1.3 スコープ

| 対象 | 変更種別 |
|---|---|
| `app/src/main/AndroidManifest.xml` | 変更（権限追加・FileProvider宣言） |
| `gradle/libs.versions.toml` | 変更（ML Kit バージョン追加） |
| `app/build.gradle.kts` | 変更（ML Kit 依存追加） |
| `ui/main/MainUiState.kt` | 変更（フィールド追加） |
| `ui/main/MainViewModel.kt` | 変更（メソッド追加） |
| `ui/main/MainScreen.kt` | 変更（カメラUI追加） |
| `data/ocr/OcrRepository.kt` | 新規作成 |
| `data/ocr/OcrRepositoryImpl.kt` | 新規作成 |
| `data/ocr/BloodPressureOcrParser.kt` | 新規作成 |
| `di/OcrModule.kt` | 新規作成 |
| `res/xml/file_paths.xml` | 新規作成 |

---

## 2. アーキテクチャパターン & バウンダリマップ

### 2.1 コンポーネント構成図

```
┌─────────────────────────────────────────────────────────────┐
│                         UI Layer                            │
│                                                             │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  MainScreen（拡張）                                   │  │
│  │  ・カメラアイコンボタン                               │  │
│  │  ・権限リクエストランチャー（RequestPermission）      │  │
│  │  ・撮影ランチャー（TakePicture）                      │  │
│  │  ・OCR処理中ローディング表示                          │  │
│  │  ・OCR自動入力バッジ                                  │  │
│  │  ・OcrErrorDialog（再撮影 / 手動入力 選択）           │  │
│  └─────────────────────────┬─────────────────────────────┘  │
│                            │ StateFlow<MainUiState>          │
│                            ▼                                │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  MainViewModel（拡張）                                │  │
│  │  ・onCameraButtonClick()                              │  │
│  │  ・onImageCaptured(uri: Uri)                          │  │
│  │  ・onOcrRetry()                                       │  │
│  │  ・onOcrDismiss()                                     │  │
│  └─────────────────────────┬─────────────────────────────┘  │
└───────────────────────────┼─────────────────────────────────┘
                            │ suspend fun analyzeImage(Uri)
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                       Data Layer                            │
│                                                             │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  OcrRepository（インターフェース）                    │  │
│  │  suspend fun analyzeImage(uri: Uri): OcrResult        │  │
│  └─────────────────────────┬─────────────────────────────┘  │
│                            │ 実装                            │
│  ┌─────────────────────────▼─────────────────────────────┐  │
│  │  OcrRepositoryImpl（ML Kit Text Recognition）         │  │
│  │  ・TextRecognizer（ML Kit クライアント）               │  │
│  │  ・InputImage.fromFilePath(context, uri)              │  │
│  │  ・recognizer.process(image): Text                    │  │
│  └─────────────────────────┬─────────────────────────────┘  │
│                            │ rawText: String                 │
│  ┌─────────────────────────▼─────────────────────────────┐  │
│  │  BloodPressureOcrParser（純粋ロジック）                │  │
│  │  fun parse(text: String): OcrResult                   │  │
│  │  ・スラッシュ区切りパターン解析                       │  │
│  │  ・数値バリデーション（要件 3.1〜3.4）                │  │
│  └───────────────────────────────────────────────────────┘  │
│                                                             │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  OcrModule（Hilt DI）                                 │  │
│  │  ・OcrRepository → OcrRepositoryImpl バインド         │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                            │
┌─────────────────────────────────────────────────────────────┐
│                    Android System                           │
│  ・システムカメラアプリ（TakePicture Intent）               │
│  ・FileProvider（一時画像URI生成）                          │
│  ・ML Kit Text Recognition（バンドル型）                    │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 データフロー（ハッピーパス）

```
ユーザー
  │ カメラボタンタップ
  ▼
MainScreen
  │ → MainViewModel.onCameraButtonClick()
  │   ├─ CAMERA権限あり → TakePictureLauncher.launch(uri)
  │   └─ CAMERA権限なし → PermissionLauncher.launch(CAMERA)
  │
  │ [システムカメラ起動・撮影]
  │
  │ TakePictureLauncher コールバック (success=true)
  │ → MainViewModel.onImageCaptured(uri)
  │   └─ uiState: isAnalyzing=true
  │     → OcrRepository.analyzeImage(uri)
  │       → ML Kit: InputImage.fromFilePath(context, uri)
  │       → recognizer.process(image): Text
  │       → BloodPressureOcrParser.parse(text): OcrResult.Success
  │     → uiState: isAnalyzing=false, systolic/diastolic/pulse 更新, isOcrFilled=true
  ▼
MainScreen: 入力フィールドに値が自動入力、OCRバッジ表示
```

### 2.3 データフロー（エラーパス）

```
OcrRepository.analyzeImage(uri)
  └─ BloodPressureOcrParser.parse(text): OcrResult.Failure(reason)
    → uiState: isAnalyzing=false, showOcrRetryDialog=true, ocrErrorMessage=メッセージ

OcrErrorDialog 表示
  ├─ 「再撮影」 → MainViewModel.onOcrRetry() → TakePictureLauncher.launch(uri)
  └─ 「手動で入力」 → MainViewModel.onOcrDismiss() → ダイアログ閉じる
```

---

## 3. テクノロジースタック & 整合性

| 技術 | バージョン | 用途 | 既存との整合性 |
|---|---|---|---|
| ML Kit Text Recognition | 16.0.1（バンドル型） | OCR エンジン | 新規追加（オンデバイス・無料） |
| ActivityResultContracts.TakePicture | androidx.activity 1.8.2（既存） | カメラ起動 | 既存 activityCompose 依存に含まれる |
| ActivityResultContracts.RequestPermission | 同上 | 権限リクエスト | 既存に含まれる |
| FileProvider | androidx.core 1.12.0（既存） | 一時画像URI生成 | 既存 core-ktx 依存に含まれる |
| Hilt 2.48.1 | 既存 | DI | OcrModule を追加するのみ |
| Kotlin Coroutines 1.7.3 | 既存 | OCR 非同期処理 | 既存 viewModelScope.launch を流用 |

### 依存追加（`gradle/libs.versions.toml`）

```toml
[versions]
mlkitTextRecognition = "16.0.1"

[libraries]
mlkit-text-recognition = { group = "com.google.mlkit", name = "text-recognition", version.ref = "mlkitTextRecognition" }
```

### 依存追加（`app/build.gradle.kts`）

```kotlin
implementation(libs.mlkit.text.recognition)
```

---

## 4. コンポーネント & インターフェース契約

### 4.1 型定義

#### `OcrResult`（sealed class）

```kotlin
// data/ocr/OcrResult.kt
sealed class OcrResult {
    data class Success(val values: BloodPressureValues) : OcrResult()
    data class Failure(val reason: OcrFailureReason) : OcrResult()
}

data class BloodPressureValues(
    val systolic: Int,   // 収縮期血圧
    val diastolic: Int,  // 拡張期血圧
    val pulse: Int       // 脈拍
)

enum class OcrFailureReason {
    NO_TEXT_DETECTED,       // テキスト未検出
    VALUES_OUT_OF_RANGE,    // バリデーション範囲外（要件 3.1〜3.3）
    INVALID_COMBINATION,    // 収縮期≦拡張期（要件 3.4）
    INSUFFICIENT_VALUES     // 3値を識別できなかった
}
```

#### `MainUiState`（拡張）

```kotlin
// ui/main/MainUiState.kt
data class MainUiState(
    // 既存フィールド（変更なし）
    val systolic: String = "",
    val diastolic: String = "",
    val pulse: String = "",
    val isSubmitting: Boolean = false,
    val resultMessage: String? = null,
    val isError: Boolean = false,
    // OCR追加フィールド
    val isAnalyzing: Boolean = false,           // OCR処理中（要件 2.2, 6.4）
    val isOcrFilled: Boolean = false,           // OCR自動入力済み（要件 4.1）
    val ocrErrorMessage: String? = null,        // OCRエラーメッセージ（要件 5.1）
    val showOcrRetryDialog: Boolean = false,    // 再撮影ダイアログ（要件 5.2）
    val cameraAvailable: Boolean = true         // カメラ利用可否（要件 5.4, 6.2）
)
```

---

### 4.2 `OcrRepository` インターフェース

```kotlin
// data/ocr/OcrRepository.kt
interface OcrRepository {
    /**
     * 指定URIの画像を解析し、血圧値を認識する。
     * @param uri FileProvider経由の画像URI
     * @return OcrResult.Success（認識成功） または OcrResult.Failure（失敗理由付き）
     */
    suspend fun analyzeImage(uri: Uri): OcrResult
}
```

**要件トレーサビリティ**: 要件 2.1, 2.3, 3.1〜3.4

---

### 4.3 `OcrRepositoryImpl` コントラクト

```kotlin
// data/ocr/OcrRepositoryImpl.kt
@Singleton
class OcrRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val parser: BloodPressureOcrParser
) : OcrRepository {

    private val recognizer: TextRecognizer =
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun analyzeImage(uri: Uri): OcrResult
    // 1. InputImage.fromFilePath(context, uri) でInputImage生成
    // 2. recognizer.process(image) を suspendCoroutine でラップして実行
    // 3. 取得した Text.text 全体を parser.parse() に渡す
    // 4. parser の結果を返す
    // 5. 例外発生時は OcrResult.Failure(NO_TEXT_DETECTED) を返す
}
```

**実装上の注意**:
- `recognizer.process()` は `Task<Text>` を返す → `suspendCoroutine { cont -> task.addOnSuccessListener {...}.addOnFailureListener {...} }` でラップ
- `InputImage.fromFilePath` は IO 例外を throw する可能性があるため try-catch 必須

---

### 4.4 `BloodPressureOcrParser` コントラクト

```kotlin
// data/ocr/BloodPressureOcrParser.kt
class BloodPressureOcrParser @Inject constructor() {

    companion object {
        const val SYSTOLIC_MIN = 60;  const val SYSTOLIC_MAX = 300
        const val DIASTOLIC_MIN = 30; const val DIASTOLIC_MAX = 200
        const val PULSE_MIN = 20;     const val PULSE_MAX = 300
    }

    /**
     * OCR認識テキストを解析し血圧値を抽出・バリデーションする。
     * @param text ML Kit が返した認識テキスト全体
     * @return OcrResult
     */
    fun parse(text: String): OcrResult

    // 内部パース戦略（優先順位順）:
    //   1. スラッシュ区切りパターン検索: "(\d+)/(\d+)" → 収縮期/拡張期
    //   2. 残りの数値から脈拍を識別（PULSE_MIN〜PULSE_MAX の範囲）
    //   3. 上記で3値が揃わない場合: 全数値リストから範囲で識別
    //   4. 値が3つ揃ったらバリデーション（要件 3.1〜3.4）
    //   5. 失敗理由を付与して OcrResult.Failure を返す
}
```

**テスト方針**: 純粋関数（Context不要）のため、JUnit4 ユニットテストで各パターンを網羅する。

---

### 4.5 `MainViewModel` 追加メソッド

```kotlin
// ui/main/MainViewModel.kt（拡張）

/**
 * カメラボタンタップ時。
 * UIに権限状況に応じたアクション（権限要求 or カメラ起動）を通知するために
 * イベントを StateFlow 経由で発行するか、コールバックを使用する。
 * 要件: 1.1, 1.2
 */
fun onCameraButtonClick(): CameraAction
// → CameraAction.RequestPermission または CameraAction.LaunchCamera(uri: Uri)

enum class CameraAction { RequestPermission, LaunchCamera }

/**
 * 撮影完了時のコールバック。
 * @param uri 撮影画像のFileProvider URI。null の場合はキャンセル（要件 1.5）
 */
fun onImageCaptured(uri: Uri?)

/**
 * OCRエラーダイアログで「再撮影」を選択したとき（要件 5.2）
 */
fun onOcrRetry()

/**
 * OCRエラーダイアログで「手動で入力」を選択したとき（要件 5.2）
 */
fun onOcrDismiss()
```

**State更新ルール**:

| 操作 | isAnalyzing | isOcrFilled | showOcrRetryDialog | ocrErrorMessage |
|---|---|---|---|---|
| `onImageCaptured(uri)` 開始時 | `true` | 変化なし | `false` | `null` |
| OCR成功時 | `false` | `true` | `false` | `null` |
| OCR失敗時 | `false` | 変化なし | `true` | メッセージ |
| `onOcrDismiss()` | - | - | `false` | `null` |
| フォールド編集時（`onSystolicChanged`等） | - | `false` | - | - |

---

### 4.6 `MainScreen` UI追加仕様

#### カメラボタン配置

```
[血圧記録] タイトル下部に水平 Row:
  ┌────────────────────────────────────────┐
  │ カメラで入力                    [📷]  │
  └────────────────────────────────────────┘
```

- `IconButton` に `Icons.Default.CameraAlt` を使用
- `enabled = !uiState.isSubmitting && !uiState.isAnalyzing && uiState.cameraAvailable` （要件 6.2, 6.4）

#### OCR処理中ローディング（要件 2.2）

```kotlin
if (uiState.isAnalyzing) {
    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
}
```

#### OCR自動入力バッジ（要件 4.1）

`isOcrFilled == true` かつフィールドが空でない場合、テキストフィールドの `supportingText` に「📷 カメラから自動入力」バッジを表示する。
ユーザーがフィールドを編集すると `isOcrFilled` が `false` になりバッジは消える。

#### OcrErrorDialog（要件 5.1, 5.2）

```kotlin
if (uiState.showOcrRetryDialog) {
    AlertDialog(
        title = { Text("認識できませんでした") },
        text = { Text(uiState.ocrErrorMessage ?: "数値を読み取れませんでした") },
        confirmButton = { Button(onClick = onOcrRetry) { Text("再撮影") } },
        dismissButton = { TextButton(onClick = onOcrDismiss) { Text("手動で入力") } },
        onDismissRequest = onOcrDismiss
    )
}
```

#### Launcher 宣言（要件 1.2〜1.4）

```kotlin
val permissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
) { granted ->
    if (granted) takePictureLauncher.launch(viewModel.prepareCaptureUri(context))
    else viewModel.onPermissionDenied()
}

val takePictureLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.TakePicture()
) { success ->
    viewModel.onImageCaptured(if (success) captureUri else null)
}
```

---

### 4.7 `OcrModule` DI

```kotlin
// di/OcrModule.kt
@Module
@InstallIn(SingletonComponent::class)
abstract class OcrModule {
    @Binds
    @Singleton
    abstract fun bindOcrRepository(impl: OcrRepositoryImpl): OcrRepository
}
```

---

### 4.8 AndroidManifest.xml 変更仕様

```xml
<!-- CAMERA権限（要件 1.2, 1.3） -->
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera" android:required="false" />

<!-- FileProvider（一時画像URI） -->
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="com.example.bloodpressuretracking.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

### 4.9 `res/xml/file_paths.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <cache-path name="ocr_images" path="ocr/" />
</paths>
```

一時画像ファイルパス: `context.cacheDir/ocr/temp_capture.jpg`

---

## 5. 要件トレーサビリティ

| 要件ID | 要件概要 | 対応コンポーネント |
|---|---|---|
| 1.1 | カメラボタンタップ → カメラ起動 | `MainScreen`（カメラボタン）、`MainViewModel.onCameraButtonClick()` |
| 1.2 | カメラ権限要求 | `MainScreen`（`RequestPermission` Launcher）、`AndroidManifest.xml` |
| 1.3 | 権限拒否 → メッセージ表示・手動入力へ | `MainViewModel.onPermissionDenied()`、`MainUiState.resultMessage` |
| 1.4 | シャッタータップ → 画像取得 | `MainScreen`（`TakePicture` Launcher）、`MainViewModel.onImageCaptured()` |
| 1.5 | キャンセル → 入力値保持 | `MainViewModel.onImageCaptured(null)` → 状態変更なし |
| 2.1 | 撮影完了 → 画像解析 | `MainViewModel.onImageCaptured(uri)`、`OcrRepositoryImpl.analyzeImage()` |
| 2.2 | 解析中ローディング | `MainUiState.isAnalyzing`、`MainScreen`（LinearProgressIndicator） |
| 2.3 | 3値個別識別 | `BloodPressureOcrParser.parse()` |
| 2.4 | 認識成功 → フォーム自動入力 | `MainViewModel`（`uiState.systolic/diastolic/pulse` 更新） |
| 3.1 | 収縮期範囲バリデーション | `BloodPressureOcrParser`（`SYSTOLIC_MIN/MAX`） |
| 3.2 | 拡張期範囲バリデーション | `BloodPressureOcrParser`（`DIASTOLIC_MIN/MAX`） |
| 3.3 | 脈拍範囲バリデーション | `BloodPressureOcrParser`（`PULSE_MIN/MAX`） |
| 3.4 | 収縮期 > 拡張期 チェック | `BloodPressureOcrParser`（`INVALID_COMBINATION`） |
| 4.1 | 自動入力視覚表示 | `MainUiState.isOcrFilled`、`MainScreen`（supportingText バッジ） |
| 4.2 | 認識値の直接編集 | 既存 `OutlinedTextField` + `onSystolicChanged` 等（変更なし） |
| 4.3 | 手動上書き可能 | 編集時 `MainViewModel` が `isOcrFilled=false` に更新 |
| 4.4 | 修正値を登録データとして扱う | 既存 `onSubmitClick()` がそのまま動作（変更なし） |
| 5.1 | 認識失敗メッセージ | `MainUiState.ocrErrorMessage`、`OcrErrorDialog` |
| 5.2 | 再撮影 / 手動入力選択肢 | `OcrErrorDialog`（confirmButton / dismissButton） |
| 5.3 | 撮影条件ガイダンス | システムカメラアプリのUI（アプリ側では `ocrErrorMessage` に補足文言） |
| 5.4 | カメラ利用不可時の誘導 | `MainUiState.cameraAvailable=false`、`MainViewModel.onCameraButtonClick()` |
| 6.1 | カメラボタン表示 | `MainScreen`（IconButton） |
| 6.2 | カメラ利用可能時のみ有効 | `MainScreen`（enabled = `cameraAvailable && !isAnalyzing && !isSubmitting`） |
| 6.3 | 手動入力のみの利用維持 | 既存フローを変更しない設計 |
| 6.4 | 認識処理中のボタン無効化 | `MainScreen`（`isAnalyzing` 時にカメラ・送信ボタン無効） |

---

## 6. リスクと対策

| リスク | 影響度 | 対策 |
|---|---|---|
| 7セグメント数字の誤認識（例: 8→0） | 高 | バリデーション範囲で異常値を検出、再撮影誘導 |
| 複数の数値が画面に混在（ラベル等） | 高 | スラッシュパターン優先 + 範囲フィルタによる識別 |
| 暗所・ピンボケで認識不可 | 中 | `OcrErrorDialog` で再撮影を促す文言 |
| カメラ権限拒否 | 低 | `onPermissionDenied()` で手動入力へ誘導（要件 1.3） |
| ML Kit 初回起動時のウォームアップ遅延 | 低 | `OcrRepositoryImpl` を `@Singleton` で事前インスタンス化 |
| `InputImage.fromFilePath` の IO 例外 | 低 | try-catch で `OcrResult.Failure(NO_TEXT_DETECTED)` に変換 |
