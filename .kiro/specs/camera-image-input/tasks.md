# 実装タスク: camera-image-input

## 要件カバレッジ

全要件対応: 1.1, 1.2, 1.3, 1.4, 1.5, 2.1, 2.2, 2.3, 2.4, 3.1, 3.2, 3.3, 3.4, 4.1, 4.2, 4.3, 4.4, 5.1, 5.2, 5.3, 5.4, 6.1, 6.2, 6.3, 6.4

---

## タスク一覧

### 1. ビルド環境とマニフェストを設定する

ML Kit 依存の追加と Android システム設定（権限・FileProvider）を行い、カメラ機能と OCR の動作基盤を整える。後続タスクすべての前提条件。

- [x] 1.1 `gradle/libs.versions.toml` に ML Kit Text Recognition のバージョン（`16.0.1`）とライブラリ定義を追加する
- [x] 1.2 `app/build.gradle.kts` に ML Kit 依存（`libs.mlkit.text.recognition`）を追加する
- [x] 1.3 `app/src/main/AndroidManifest.xml` に `CAMERA` 権限、カメラ feature 宣言、FileProvider プロバイダーを追加する
  - 要件カバレッジ: 1.2, 1.3, 5.4
- [x] 1.4 `app/src/main/res/xml/file_paths.xml` を新規作成し、`cache-path`（`ocr/` ディレクトリ）を定義する

---

### 2. OCR データ層を実装する

血圧値を OCR で取得するためのデータ層コンポーネント（型定義・インターフェース・パーサー・ML Kit 実装・DI モジュール）を新規作成する。

- [x] 2.1 `data/ocr/OcrResult.kt` に `OcrResult` sealed class、`BloodPressureValues` データクラス、`OcrFailureReason` enum を定義する
- [x] 2.2 `data/ocr/OcrRepository.kt` に `OcrRepository` インターフェース（`suspend fun analyzeImage(uri: Uri): OcrResult`）を作成する
  - 要件カバレッジ: 2.1, 2.3
- [x] 2.3 `data/ocr/BloodPressureOcrParser.kt` を実装する。OCR テキストからスラッシュ区切りパターンを優先的に解析し、3値（収縮期・拡張期・脈拍）を抽出してバリデーションを行う
  - 要件カバレッジ: 2.3, 3.1, 3.2, 3.3, 3.4
- [x] 2.4 `data/ocr/OcrRepositoryImpl.kt` を実装する。ML Kit `TextRecognition.getClient()` で認識器を生成し、`InputImage.fromFilePath(context, uri)` で画像を読み込み、`suspendCoroutine` で Task を Coroutine に変換して `BloodPressureOcrParser` に委譲する
  - 要件カバレッジ: 2.1, 2.4, 5.1
- [x] 2.5 `di/OcrModule.kt` を作成し、`OcrRepository → OcrRepositoryImpl` の Hilt バインドを `@Singleton` スコープで登録する

---

### 3. MainUiState と MainViewModel を拡張する

UI 状態に OCR 関連フィールドを追加し、カメラ起動・OCR 実行・結果処理のビジネスロジックを ViewModel に実装する。

- [x] 3.1 `MainUiState` に OCR 用フィールド（`isAnalyzing`、`isOcrFilled`、`ocrErrorMessage`、`showOcrRetryDialog`、`cameraAvailable`）を追加する
  - 要件カバレッジ: 2.2, 4.1, 5.1, 5.2, 5.4, 6.2, 6.4
- [x] 3.2 `MainViewModel` に以下のメソッドを追加する。`prepareCaptureUri(context)`（FileProvider URI 生成）、`onCameraButtonClick()`（権限状態を判定して `CameraAction` を返す）、`onPermissionDenied()`（要件 1.3 対応）、`onImageCaptured(uri)`（OCR 起動・状態更新）、`onOcrRetry()`、`onOcrDismiss()`。また `onSystolicChanged` / `onDiastolicChanged` / `onPulseChanged` の編集時に `isOcrFilled = false` となるよう更新する
  - 要件カバレッジ: 1.1, 1.2, 1.3, 1.4, 1.5, 2.1, 2.2, 2.4, 4.3, 4.4, 5.1, 5.2, 5.4

---

### 4. MainScreen にカメラ UI を追加する

メイン画面にカメラボタン・権限/撮影ランチャー・ローディング表示・OCR バッジ・エラーダイアログを追加し、既存の手動入力フローを維持したまま統合する。

- [x] 4.1 `MainScreen` に `rememberLauncherForActivityResult(RequestPermission)` と `rememberLauncherForActivityResult(TakePicture)` を宣言し、ViewModel の `onCameraButtonClick()` 結果に応じて適切なランチャーを呼び出すロジックを実装する
  - 要件カバレッジ: 1.1, 1.2, 1.3, 1.4, 1.5
- [x] 4.2 タイトル下部にカメラボタン（`Icons.Default.CameraAlt`）を追加し、`isAnalyzing` 中には `LinearProgressIndicator` を表示する。送信ボタンおよびカメラボタンの `enabled` 条件に `isAnalyzing` を追加する
  - 要件カバレッジ: 2.2, 5.4, 6.1, 6.2, 6.3, 6.4
- [x] 4.3 収縮期・拡張期・脈拍の各 `OutlinedTextField` の `supportingText` に、`isOcrFilled == true` の場合のみ「カメラから自動入力」バッジを表示する
  - 要件カバレッジ: 4.1, 4.2
- [x] 4.4 `showOcrRetryDialog == true` の場合に `AlertDialog`（`OcrErrorDialog`）を表示する。確認ボタンで再撮影、キャンセルボタンで手動入力を選択できるようにする。エラーメッセージには撮影条件改善のガイダンス文言を含める
  - 要件カバレッジ: 5.1, 5.2, 5.3

---

### 5. BloodPressureOcrParser のユニットテストを作成する

パーサーは Context 不要の純粋関数であるため、JUnit4 で複数の OCR 出力パターンを網羅的にテストする。

- [x] 5.1 `BloodPressureOcrParserTest` を作成し、スラッシュ区切りパターン・3行表示パターン・範囲外バリデーション（収縮期・拡張期・脈拍）・収縮期≦拡張期の組み合わせ異常・テキスト未検出の各ケースを検証する
  - 要件カバレッジ: 2.3, 3.1, 3.2, 3.3, 3.4

---

## タスク依存関係

```
タスク1（ビルド環境）
  └─ タスク2（OCR データ層）
       └─ タスク3（ViewModel 拡張）
            └─ タスク4（UI 拡張）
タスク2.3（BloodPressureOcrParser）
  └─ タスク5（ユニットテスト）  ← タスク4と並行実施可能
```
