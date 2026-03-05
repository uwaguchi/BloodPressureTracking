# ギャップ分析レポート: camera-image-input

## エグゼクティブサマリー

| 項目 | 状況 |
|------|------|
| 分析対象 | カメラ撮影 → OCR → 血圧値自動入力 |
| 既存コードの流用度 | 中（UI層・ViewModel層は拡張可能） |
| 新規実装が必要なもの | カメラ機能、OCRエンジン、DI追加 |
| 推奨アプローチ | CameraX（Intent方式） + ML Kit Text Recognition |
| 主なリスク | OCR精度（血圧計フォント・レイアウト依存） |

---

## 1. 既存コードの現状

### 1.1 活用できるコンポーネント

| コンポーネント | ファイル | 活用方法 |
|---|---|---|
| `MainUiState` | `ui/main/MainUiState.kt` | カメラ状態・OCR状態フィールドを追加拡張 |
| `MainViewModel` | `ui/main/MainViewModel.kt` | カメラトリガー・OCR結果受け取りメソッド追加 |
| `MainScreen` | `ui/main/MainScreen.kt` | カメラボタン・ローディング表示追加 |
| Hilt DI基盤 | `di/RepositoryModule.kt` | OCRリポジトリのバインド追加 |
| Coroutines | 全VM | OCR非同期処理に既存 `viewModelScope.launch` を流用 |

### 1.2 存在しないコンポーネント（新規実装必要）

| 欠けているもの | 要件との対応 |
|---|---|
| カメラ権限宣言 | 要件 1.2, 1.3 |
| カメラ起動ロジック | 要件 1.1, 1.4, 1.5 |
| OCRエンジン | 要件 2.1〜2.4 |
| 数値バリデーションロジック | 要件 3.1〜3.4（一部手動入力用が存在するが不完全） |
| OCRリポジトリ / DI | 要件 2.1 |
| エラーUI（再撮影ダイアログ等） | 要件 5.1〜5.4 |
| 自動入力視覚フィードバック | 要件 4.1 |

---

## 2. 技術的ギャップの詳細

### 2.1 AndroidManifest（`app/src/main/AndroidManifest.xml`）

**現状**:
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

**必要な追加**:
```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera" android:required="false" />
```

---

### 2.2 `MainUiState.kt` の拡張

**現状**:
```kotlin
data class MainUiState(
    val systolic: String = "",
    val diastolic: String = "",
    val pulse: String = "",
    val isSubmitting: Boolean = false,
    val resultMessage: String? = null,
    val isError: Boolean = false
)
```

**追加が必要なフィールド**:
```kotlin
val isAnalyzing: Boolean = false,          // OCR処理中フラグ（要件 2.2, 6.4）
val isOcrFilled: Boolean = false,          // OCR自動入力済みフラグ（要件 4.1）
val ocrErrorMessage: String? = null,       // OCR失敗メッセージ（要件 5.1）
val showOcrRetryDialog: Boolean = false,   // 再撮影ダイアログ表示（要件 5.2）
```

---

### 2.3 `MainViewModel.kt` の拡張

**追加が必要なメソッド**:
- `onCameraButtonClick()` — カメラ起動トリガー
- `onImageCaptured(uri: Uri)` — 撮影画像受取り → OCR実行
- `onOcrResult(systolic: Int?, diastolic: Int?, pulse: Int?)` — OCR結果フォームへ反映
- `onOcrRetry()` — 再撮影ダイアログのアクション
- `clearOcrState()` — OCR状態クリア

---

### 2.4 `MainScreen.kt` の拡張

**追加が必要なUI要素**:
- カメラアイコンボタン（要件 6.1, 6.2）
- `ActivityResultLauncher` による権限リクエストと撮影（要件 1.2〜1.4）
- OCR処理中ローディング（要件 2.2）
- OCR自動入力済みバッジ/ハイライト（要件 4.1）
- エラーダイアログ（再撮影 / 手動入力選択）（要件 5.1, 5.2）

---

### 2.5 新規作成が必要なファイル

| ファイル | 役割 |
|---|---|
| `data/ocr/OcrRepository.kt` | OCR処理インターフェース |
| `data/ocr/OcrRepositoryImpl.kt` | ML Kit実装 |
| `data/ocr/BloodPressureOcrParser.kt` | OCR結果テキスト → 血圧値パース・バリデーション |
| `di/OcrModule.kt` | Hilt DI バインド |

---

## 3. OCRライブラリの選択肢

### 選択肢A: ML Kit Text Recognition（推奨）

| 項目 | 内容 |
|---|---|
| 種類 | オンデバイス処理（ネットワーク不要） |
| コスト | 無料 |
| 精度 | 印刷文字・デジタル表示に強い |
| 追加依存 | `com.google.mlkit:text-recognition:16.0.x` |
| 統合難易度 | 低（公式Android APIとして整備済み） |
| オフライン | 対応 |

**推奨理由**: 血圧計のデジタル数字（7セグメント表示）は明瞭で ML Kit が得意とする文字形状。オンデバイスのためレイテンシが低く、プライバシーに配慮できる。

### 選択肢B: Google Cloud Vision API

| 項目 | 内容 |
|---|---|
| 種類 | クラウドAPI |
| コスト | 月1000件まで無料、以降有料 |
| 精度 | 非常に高い |
| 追加依存 | HTTP通信（既存OkHttp活用可） |
| オフライン | 非対応 |

**非推奨理由**: API キー管理が必要、ネットワーク依存（オフライン不可）、コスト発生リスク。

### 選択肢C: Intent経由のシステムカメラ + Tessaract OCR

**非推奨理由**: Tessaract は Android での統合が複雑でパフォーマンスに難がある。

---

## 4. カメラ起動方式の選択肢

### 選択肢A: `ActivityResultContracts.TakePicture()` / Intent（推奨）

- システムカメラアプリを起動
- 実装が最もシンプル
- `FileProvider` でURIを受け取り ML Kit に渡す
- Compose との親和性: `rememberLauncherForActivityResult` で対応

### 選択肢B: CameraX（`androidx.camera`）

- アプリ内カメラUI（プレビュー込み）
- 撮影構図ガイド表示が可能（要件5.3と相性良い）
- 依存ライブラリ追加が必要（`camera-camera2`, `camera-lifecycle`, `camera-view`）
- 実装コスト高

**推奨**: まず選択肢A（Intent方式）で要件を満たし、ガイダンス表示などUX向上が必要であれば選択肢Bへ移行する段階的アプローチ。

---

## 5. `build.gradle.kts` への追加依存

```kotlin
// ML Kit Text Recognition
implementation("com.google.mlkit:text-recognition:16.0.1")

// CameraX（選択肢Bを選んだ場合のみ）
// implementation("androidx.camera:camera-camera2:1.3.x")
// implementation("androidx.camera:camera-lifecycle:1.3.x")
// implementation("androidx.camera:camera-view:1.3.x")
```

---

## 6. 主なリスクと対策

| リスク | 影響度 | 対策 |
|---|---|---|
| OCR精度が低い（血圧計フォントによる） | 高 | バリデーション（要件3）+ 再撮影誘導（要件5.2）でカバー |
| 暗所・ピンボケで認識不可 | 中 | 撮影ガイダンス表示（要件5.3） |
| カメラ権限拒否 | 低 | フォールバックとして手動入力維持（要件6.3） |
| ML Kitモデルダウンロード遅延 | 低 | 初回起動時にプリロード可能 |
| 7セグメント数字の誤認識（例：6→0） | 中 | バリデーション範囲チェックで検出 + ユーザー確認フロー |

---

## 7. 統合ポイントのまとめ

```
MainScreen.kt
  ├── カメラボタン追加
  ├── rememberLauncherForActivityResult（権限リクエスト）
  ├── rememberLauncherForActivityResult（カメラ起動）
  ├── isAnalyzing時のローディング / ボタン無効化
  └── OCRエラーダイアログ
        │
        ↓
MainViewModel.kt（拡張）
  ├── onCameraButtonClick()
  ├── onImageCaptured(uri) → OcrRepository.analyze(uri)
  └── onOcrResult() → UiState更新
        │
        ↓
OcrRepository（新規）
  ├── OcrRepositoryImpl（ML Kit）
  └── BloodPressureOcrParser（テキスト→数値・バリデーション）
        │
        ↓
OcrModule.kt（新規 Hilt DI）
```

---

## 8. 次フェーズへの推奨事項

1. **OCRライブラリ**: ML Kit Text Recognition を採用（オンデバイス・無料）
2. **カメラ方式**: Intent + ActivityResultContracts でシンプルに実装
3. **段階的移行**: 既存 `MainUiState` / `MainViewModel` を拡張する形（新規画面不要）
4. **テスト方針**: `BloodPressureOcrParser` は純粋ロジックなのでユニットテスト容易
