# コードドキュメント：`BrowserDisplayScreen.java`

## 🧩 概要
このクラス `BrowserDisplayScreen` は、**Minecraft 内部で動作する仮想ブラウザ画面を表示するための GUI クラス**である。  
HTMLライクなテキスト構造（例: `<b>`, `<i>`, `<h1>`, `<color=#RRGGBB>`, `<br>` など）を独自にパースして整形し、スタイル付きテキストを Minecraft のフォントシステム上で描画する機能を持つ。

主な特徴は次の通り：
- テキストを HTML 風のタグでスタイリング可能（色・太字・見出しなど）
- スクロール操作（マウスホイール）による長文の閲覧
- `Back` ボタンによる画面閉鎖処理
- URL が含まれる場合（未実装想定）にクリックでブラウザを開く拡張性

---

## 🧾 グローバル変数一覧

| 変数名 | 型 | 初期値 | 使用箇所 | 説明 |
|--------|----|--------|----------|------|
| `content` | `String` | コンストラクタ引数 | `init()`, `parseHTMLLikeContent()` | 表示対象となるHTMLライクなテキスト本文。 |
| `lines` | `List<FormattedLine>` | `new ArrayList<>()` | `parseHTMLLikeContent()`, `render()` | 1行ごとの描画データを保持（文字列＋リンク情報）。 |
| `scrollOffset` | `int` | `0` | `render()`, `mouseScrolled()` | 現在のスクロール位置（上から何行目かを示す）。 |
| `LINE_HEIGHT` | `int` | `11` | `render()`, `mouseScrolled()` | 行の高さをピクセル単位で定義。 |
| `PADDING` | `int` | `12` | `render()`, `init()`, `parseHTMLLikeContent()` | 左右の余白を調整するためのパディング値。 |

---

## ⚙️ 関数一覧

### `BrowserDisplayScreen(String content)`
- **概要**: コンストラクタ。表示対象のコンテンツを受け取り、タイトルを「Browser Display」として初期化する。
- **引数**:
    - `content`: HTMLライクなテキスト本文。
- **呼び出し元**: ブラウザ画面を開くクラス（例: `BrowserBlockEntity` → `BrowserScreen`）。
- **呼び出し先**: `Screen` クラスのスーパークラスコンストラクタ。

---

### `init()`
- **戻り値**: なし
- **概要**: 画面の初期化処理を行う。
- **主な処理**:
    1. `super.init()` で親クラス初期化。
    2. `parseHTMLLikeContent(content)` を呼び出し、テキストを行単位に分割・整形。
    3. 画面下部に「Back」ボタンを設置し、押下時に `onClose()` を呼び出す。
- **呼び出し元**: Forge の GUI 初期化イベント。
- **呼び出し先**: `parseHTMLLikeContent()` / `onClose()`

---

### `parseHTMLLikeContent(String input)`
- **戻り値**: なし
- **概要**: HTML風テキスト（`<b>`, `<i>`, `<color=#RRGGBB>`など）を解析して、Minecraft が描画できる `FormattedCharSequence` に変換。
- **主な処理**:
    1. `lines.clear()` で既存の行データをリセット。
    2. `<br>` を改行コード `\n` に置換。
    3. テキストを行単位で分割し、各行を `parseStyledText()` で解析。
    4. `this.font.split()` を使って幅に合わせて自動改行。
    5. 各行を `FormattedLine` として `lines` リストに格納。
- **呼び出し元**: `init()`
- **呼び出し先**: `parseStyledText()`

---

### `parseStyledText(String text) -> Component`
- **戻り値**: `Component`（Minecraftのスタイル付きテキストオブジェクト）
- **概要**: 行内に含まれるHTML風タグを解析し、色・太字・斜体・見出しなどの装飾を適用。
- **主な処理**:
    1. 正規表現 `<[^>]+>|[^<]+` を使用してタグとテキストを分離。
    2. 各タグを走査し、タグ種別ごとに `currentColor` や `bold`, `italic` フラグを変更。
    3. `Style` オブジェクトに現在の装飾設定を反映し、`TextComponent` として連結。
    4. 色指定タグ `<color=#xxxxxx>` を16進数RGBとして解析。
    5. 未知のタグやフォーマットエラー時はデフォルト色（白）に戻す。
- **呼び出し元**: `parseHTMLLikeContent()`
- **呼び出し先**: `TextComponent`, `Style`（Minecraft標準クラス）
- **例外処理**:
    - 色指定が不正な場合 `NumberFormatException` を `ignored` として無視。

---

### `render(PoseStack poseStack, int mouseX, int mouseY, float partialTick)`
- **戻り値**: なし
- **概要**: ブラウザ画面を描画するメイン関数。タイトル・テキスト行・ボタンを描画する。
- **主な処理**:
    1. 背景を描画 (`renderBackground()`)。
    2. タイトル `"Minecraft Browser"` を中央上部に描画。
    3. 表示範囲内の行数を計算し、`scrollOffset` に応じて部分描画。
    4. 各行を `drawString()` で順番に描画。
    5. `super.render()` でボタンなど他のウィジェットも描画。
- **呼び出し元**: Minecraft のフレーム更新ループ。
- **呼び出し先**: `drawString()`, `renderBackground()`

---

### `mouseScrolled(double mouseX, double mouseY, double delta) -> boolean`
- **概要**: マウスホイールでスクロール操作を処理。
- **主な処理**:
    1. 全体行数と表示可能行数をもとに最大オフセットを計算。
    2. `delta > 0` なら上スクロール（`scrollOffset--`）。
    3. `delta < 0` なら下スクロール（`scrollOffset++`）。
    4. 範囲外スクロールを防ぐため `Math.min()` / `Math.max()` を使用。
- **呼び出し元**: Minecraft GUIイベントハンドラ。

---

### `mouseClicked(double mouseX, double mouseY, int button) -> boolean`
- **概要**: 左クリック時にクリック位置のテキスト行を特定し、リンク付き行であれば外部ブラウザを起動する。
- **主な処理**:
    1. マウスクリック位置から行インデックスを算出。
    2. 対応する `FormattedLine` を取得。
    3. `line.link != null` の場合、`Desktop.getDesktop().browse()` でURIを開く。
    4. 例外時（URI不正等）には `printStackTrace()`。
- **呼び出し元**: Minecraft の入力イベント。
- **呼び出し先**: Java AWT `Desktop` API。

---

### `isPauseScreen() -> boolean`
- **戻り値**: `false`
- **概要**: この画面表示中もゲームがポーズ（停止）しないよう設定。

---

### `onClose()`
- **戻り値**: なし
- **概要**: 画面を閉じる処理。  
  `Minecraft.getInstance().setScreen(null)` を呼び出して通常画面へ戻す。
- **呼び出し元**: 「Back」ボタン、またはESCキー入力。

---

### 内部クラス `FormattedLine`
| フィールド | 型 | 説明 |
|------------|----|------|
| `text` | `FormattedCharSequence` | 描画対象の1行分テキスト。 |
| `link` | `String` | 行に関連付けられたリンクURL（現状はnull）。 |

- **役割**: 各描画行を保持するデータ構造体。
- **使用箇所**: `lines` リスト（`parseHTMLLikeContent()`, `render()`, `mouseClicked()`）。

---

## 🔗 呼び出し関係図
```
init()
└─ parseHTMLLikeContent(content)
└─ parseStyledText(text)
render()
├─ drawCenteredString()
├─ drawString()
└─ super.render()
mouseClicked()
└─ Desktop.getDesktop().browse()
mouseScrolled()
onClose()
└─ Minecraft.getInstance().setScreen(null)
```
---

## 🧩 外部依存関係

| クラス | パッケージ | 用途 |
|--------|-------------|------|
| `Screen` | `net.minecraft.client.gui.screens` | Minecraft の画面描画の基本クラス。 |
| `Button` | `net.minecraft.client.gui.components` | GUIボタンコンポーネント。 |
| `PoseStack` | `com.mojang.blaze3d.vertex` | 描画座標変換スタック。 |
| `TextComponent`, `Style`, `TextColor` | `net.minecraft.network.chat` | Minecraft標準の文字装飾用API。 |
| `Desktop`, `URI` | `java.awt`, `java.net` | 外部ブラウザ起動（リンククリック対応）。 |
| `Pattern`, `Matcher` | `java.util.regex` | HTML風タグの解析に使用。 |

---

## 📦 ファイル別概要

| ファイル名 | 主な責務 | 主な関数 |
|-------------|-----------|-----------|
| `BrowserDisplayScreen.java` | HTMLライクなテキストをMinecraft画面上に整形・描画するブラウザ風画面の実装。 | `init()`, `parseHTMLLikeContent()`, `parseStyledText()`, `render()` |

---

## 🧭 処理全体の流れ（ストーリー形式）

1. `BrowserDisplayScreen` が生成され、HTML風テキストが引数として渡される。
2. `init()` にて `parseHTMLLikeContent()` が呼ばれ、タグを解析して行データ（`lines`）を生成。
3. `render()` で行ごとにスタイル付きテキストを描画。
4. プレイヤーがマウスホイールを操作すると `mouseScrolled()` により上下スクロールが可能。
5. 「Back」ボタンまたはESCキーで `onClose()` が呼ばれ、ブラウザ画面を終了。
6. （将来的には）クリックイベントにより `<a>` タグなどから外部リンクを開くことも可能。

---

## ⚙️ 設計上の特徴
- **HTML風テキスト解析**: `<b>`, `<i>`, `<h1>`, `<color>` などの限定的なHTML風マークアップを再現。
- **軽量なスクロール機能**: Minecraft標準のGUI構造で実装されたシンプルな手動スクロール。
- **UI拡張性**: 将来的にリンククリック、画像埋め込みなどへ拡張可能。
- **描画最適化**: `this.font.split()` による自動改行処理で、幅に応じたレイアウトを維持。

---

## 💡 まとめ
`BrowserDisplayScreen` は、Minecraft 内で動作する「仮想ウェブブラウザ」のビューワー機能を提供するクラスである。  
この実装により、プレイヤーはサーバーや内部データから取得したHTML風テキストを、装飾付きで閲覧できる。  
ゲーム内情報端末や通信ネットワークのビジュアル出力など、インタラクティブな情報表示を行う土台となっている。