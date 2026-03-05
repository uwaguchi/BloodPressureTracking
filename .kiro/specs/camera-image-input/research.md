# リサーチログ: camera-image-input

## サマリー

- **ディスカバリ種別**: 拡張型（Extension）— 既存のMVVM + Repositoryパターンを継承し拡張
- **主要調査項目**: OCRライブラリ選定、カメラ起動方式、FileProvider設定、ML Kit API詳細
- **重要な知見**: ML Kit Text Recognition v2 の `InputImage.fromFilePath` がURI入力に直接対応しており、`ActivityResultContracts.TakePicture` との組み合わせが最適

---

## リサーチログ

### Topic 1: ML Kit Text Recognition v2 API

**出典**: [Google Developers - Text Recognition v2](https://developers.google.com/ml-kit/vision/text-recognition/v2/android)

**調査結果**:
- 最新バージョン: `com.google.mlkit:text-recognition:16.0.1`（2024年8月リリース）
- `TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)` でクライアント生成
- `InputImage.fromFilePath(context, uri)` でURIから直接InputImage生成可能（FileProvider URI対応）
- 返却される `Text` オブジェクトの構造: `Text → TextBlock → Line → Element → Symbol`
- `Task<Text>` ベースの非同期API → Coroutine `suspendCoroutine` でラップ可能
- **バンドル型**（bundled）: アプリに同梱、初回起動即利用可能（アプリサイズ増加）
- **アンバンドル型**（unbundled）: Google Play Services経由、動的ダウンロード

**設計への影響**:
- `OcrRepositoryImpl` は `InputImage.fromFilePath` を使用し、Uri を直接受け取る設計とする
- Task<T> を Kotlin Coroutine に変換するために `suspendCoroutine` または `Tasks.await()` を使用
- バンドル型を採用（血圧記録アプリはネットワーク不安定環境でも使用されうるため）

---

### Topic 2: ActivityResultContracts.TakePicture + FileProvider

**出典**: [Android Developers](https://developer.android.com/reference/kotlin/androidx/activity/result/contract/ActivityResultContracts.TakePicture)

**調査結果**:
- `ActivityResultContracts.TakePicture()` は URI を入力として受け取り、`Boolean`（成功/失敗）を返す
- 撮影前に `FileProvider.getUriForFile(context, authority, tempFile)` でURIを生成する必要がある
- Compose では `rememberLauncherForActivityResult` で宣言的に扱う
- `CAMERA` 権限は **不要**（システムカメラアプリが撮影するため）ただし `FileProvider` は必須
  - ただし要件 1.2, 1.3 でカメラ権限を扱うことが求められているため、`ActivityResultContracts.RequestPermission(CAMERA)` を追加する
  - 実際にはIntent方式では不要だが、UX面でより明示的な権限フローとする
- `res/xml/file_paths.xml` の定義と `AndroidManifest.xml` への provider 宣言が必要

**設計への影響**:
- `MainScreen` に2つの `rememberLauncherForActivityResult` を定義:
  1. `RequestPermission(Manifest.permission.CAMERA)` — 権限要求
  2. `TakePicture()` — カメラ起動
- `FileProvider` authority: `"com.example.bloodpressuretracking.fileprovider"`
- 一時画像ファイルは `context.cacheDir/ocr/temp_capture.jpg` に保存

---

### Topic 3: Gradle バージョンカタログ（libs.versions.toml）

**調査結果（既存ファイル確認済み）**:
- プロジェクトは `gradle/libs.versions.toml` を使用したバージョンカタログ方式を採用
- ML Kit は現在未登録 → バージョンカタログに追加が必要
- `minSdk = 33`（ML Kit の要件 API 21 以上を十分に満たす）

**追加が必要なエントリ**:
```toml
[versions]
mlkitTextRecognition = "16.0.1"

[libraries]
mlkit-text-recognition = { group = "com.google.mlkit", name = "text-recognition", version.ref = "mlkitTextRecognition" }
```

---

### Topic 4: 血圧計OCRパース戦略

**調査結果**:

血圧計のデジタル表示パターン（代表例）:
1. `"120/80\n60"` — 上段:収縮期/拡張期、下段:脈拍
2. `"120\n80\n60"` — 三段表示
3. `"SYS 120 DIA 80 PUL 60"` — ラベル付き横並び
4. `"120 80 60"` — スペース区切り

**パース方針**（優先順位順）:
1. スラッシュ区切りパターン検出: `(\d+)/(\d+)` → 収縮期/拡張期
2. 3つの数値を血圧範囲でフィルタして識別
3. 数値が2つしかない場合は脈拍欠損として失敗扱い

**バリデーション定数**（要件 3.1〜3.4 より）:
- 収縮期: 60〜300 mmHg
- 拡張期: 30〜200 mmHg
- 脈拍: 20〜300 bpm
- 収縮期 > 拡張期 であること

---

## アーキテクチャパターン評価

| パターン | 評価 | 理由 |
|---|---|---|
| 既存パターン拡張（MVVM + Repository） | ✅ 採用 | 最小変更、既存コードとの整合性 |
| 新規画面追加（CameraScreen） | ❌ 非採用 | 要件6.3の既存フロー維持に反する、過剰設計 |
| CameraX インApp Camera | ❌ 非採用 | 実装コスト高、Intent方式で要件充足可能 |
| Cloud Vision API | ❌ 非採用 | API キー管理、コスト、ネットワーク依存 |
