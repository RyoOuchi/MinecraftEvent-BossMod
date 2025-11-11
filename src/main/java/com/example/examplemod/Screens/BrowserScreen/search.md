# コードドキュメント：`BrowserSearchScreen.java`

## 🧩 概要
このクラス `BrowserSearchScreen` は、Minecraft 内で動作する**仮想ブラウザの検索・アドレス入力画面**を実装している。  
プレイヤーが URL を入力し「Go」ボタンを押すと、URL がサーバー側に送信され、結果的に `BrowserLoadingScreen` → `BrowserDisplayScreen` の順でウェブページの表示を行う一連の流れの起点となる。

主な処理フローは以下の通り：
1. プレイヤーが URL を入力。
2. 「Go」ボタンを押下。
3. 入力値が空でなければ：
    - クライアント側で「Requesting: ...」とメッセージ表示。
    - `BrowserBlockEntity` により URL を `byte[]` 形式に変換。
    - ローディング画面 (`BrowserLoadingScreen`) を表示。
    - ネットワーク経由で `RequestUrlPacket` をサーバーに送信。

---

## 🧾 グローバル変数一覧

| 変数名 | 型 | 初期値 | 使用箇所 | 説明 |
|--------|----|--------|----------|------|
| `urlField` | `EditBox` | `null`（初期値） | `init()`, `render()`, `keyPressed()`, `charTyped()` | プレイヤーが URL を入力するテキストボックス。 |
| `blockEntity` | `BrowserBlockEntity` | コンストラクタで受け取り | `init()` | ブラウザブロックのエンティティ。サーバー通信に必要な情報（座標など）を保持。 |

---

## ⚙️ 関数一覧

### `BrowserSearchScreen(BrowserBlockEntity blockEntity)`
- **概要**: コンストラクタ。対象となるブラウザブロックのエンティティを受け取り、画面タイトルを `"Browser"` に設定。
- **引数**:
    - `blockEntity`: 関連する `BrowserBlockEntity`（URLリクエストの送信対象）。
- **呼び出し元**: `BrowserBlock#use()`（ブラウザブロック右クリック時など）。
- **呼び出し先**: `Screen` クラスのスーパークラスコンストラクタ。

---

### `init()`
- **戻り値**: なし (`void`)
- **概要**: GUI 初期化処理。URL入力欄と「Go」ボタンを生成し、画面中央に配置。
- **主な処理**:
    1. `super.init()` で親クラス初期化。
    2. テキストボックス (`EditBox`) の生成と登録。
    3. 「Go」ボタンを追加。クリック時の処理として：
        - 入力欄の値を取得 (`urlField.getValue()`)。
        - 入力が空文字でなければ、  
          a. クライアントプレイヤーに「Requesting: URL」とメッセージ送信。  
          b. `blockEntity.convertUrlToByte(url)` によりURLをバイト配列へ変換。  
          c. `BrowserLoadingScreen` を表示。  
          d. `ExampleMod.CHANNEL.sendToServer()` により `RequestUrlPacket` を送信。
- **呼び出し元**: Minecraft の GUI 初期化イベント。
- **呼び出し先**:
    - `EditBox` コンストラクタ
    - `BrowserBlockEntity#convertUrlToByte()`
    - `ExampleMod.CHANNEL.sendToServer()`

---

### `render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks)`
- **戻り値**: なし (`void`)
- **概要**: 画面描画処理。背景、タイトル文字列、入力欄を描画。
- **主な処理**:
    1. `renderBackground(poseStack)` で背景を描画。
    2. `"Enter URL"` のタイトル文字を画面中央に描画。
    3. `super.render()` でボタンなどのウィジェットを描画。
    4. 最後に `urlField.render()` を呼び出し、入力欄を描画。
- **呼び出し元**: Minecraft の描画ループ。
- **呼び出し先**: `EditBox#render()`, `drawCenteredString()`。

---

### `keyPressed(int keyCode, int scanCode, int modifiers) -> boolean`
- **概要**: キーボード入力イベントを処理。
- **主な処理**:
    - `urlField.keyPressed()` または `urlField.canConsumeInput()` が `true` の場合、イベントを消費して `true` を返す。
    - それ以外の場合はスーパークラスへ処理を委譲。
- **呼び出し元**: Minecraft の入力イベント。

---

### `charTyped(char codePoint, int modifiers) -> boolean`
- **概要**: 文字入力時（例: URL入力中）の処理。
- **主な処理**:
    - `urlField.charTyped()` が `true` の場合、入力欄に反映。
    - それ以外の場合、親クラスへ委譲。
- **呼び出し元**: Minecraft の文字入力イベント。

---

### `isPauseScreen() -> boolean`
- **戻り値**: `false`
- **概要**: この画面が開いている間も、ゲームを一時停止しない設定。
- **目的**: URL送信中も内部処理（ネットワーク通信）を継続させる。

---

## 🔗 呼び出し関係図
```
BrowserBlockEntity (right-click)
└─ new BrowserSearchScreen(blockEntity)
├─ init()
│   ├─ addRenderableWidget(EditBox)
│   ├─ addRenderableWidget(Button)
│   │     ├─ BrowserBlockEntity.convertUrlToByte()
│   │     ├─ Minecraft.getInstance().setScreen(new BrowserLoadingScreen())
│   │     └─ ExampleMod.CHANNEL.sendToServer(new RequestUrlPacket(…))
├─ render()
├─ keyPressed()
├─ charTyped()
└─ isPauseScreen()
```
---

## 🧩 外部依存関係

| クラス名 | パッケージ | 用途 |
|-----------|-------------|------|
| `Screen` | `net.minecraft.client.gui.screens` | Minecraft の GUI 画面ベースクラス。 |
| `EditBox` | `net.minecraft.client.gui.components` | ユーザー入力欄（テキストフィールド）。 |
| `Button` | `net.minecraft.client.gui.components` | クリック可能ボタンの実装。 |
| `PoseStack` | `com.mojang.blaze3d.vertex` | 描画位置や座標の管理。 |
| `BrowserBlockEntity` | `com.example.examplemod.Blocks.BrowserBlock` | ブラウザブロックのサーバー側エンティティ。URL変換や座標保持。 |
| `RequestUrlPacket` | `com.example.examplemod.Packet` | クライアント → サーバー間でURLリクエストを送信するパケット。 |
| `BrowserLoadingScreen` | `com.example.examplemod.Screens.BrowserScreen` | ローディング中アニメーションを表示する画面。 |
| `ExampleMod.CHANNEL` | `com.example.examplemod` | Forge の `SimpleChannel`。パケット送信処理に使用。 |
| `TextComponent` | `net.minecraft.network.chat` | GUI要素のテキスト描画。 |

---

## 📦 ファイル別概要

| ファイル名 | 主な責務 | 主な関数 |
|-------------|-----------|-----------|
| `BrowserSearchScreen.java` | URL入力画面。プレイヤーがURLを入力し、サーバーにアクセスリクエストを送信する。 | `init()`, `render()`, `keyPressed()`, `charTyped()` |

---

## 🧭 処理全体の流れ（ストーリー形式）

1. プレイヤーがブラウザブロックを右クリックすると、`BrowserSearchScreen` が開く。
2. プレイヤーは URL を入力欄に入力する。
3. 「Go」ボタンを押すと：
    - 入力内容が空でなければ、クライアントチャットに「Requesting: URL」と表示。
    - ブロックエンティティによりURLをバイトデータ化。
    - `BrowserLoadingScreen` が開き、「Loading...」アニメーションが表示される。
    - `RequestUrlPacket` がサーバーへ送信され、サーバーが該当URLのデータ取得を処理。
4. サーバーがデータを返すと、`BrowserDisplayScreen` で結果がレンダリングされる。

---

## ⚙️ 設計上の特徴

- **直感的なUI設計**  
  シンプルなテキストボックス＋ボタン構成で、URL入力操作を直感的に行える。
- **非同期通信設計**  
  パケット送信後にローディング画面へ即時遷移することで、処理待ちのUXを向上。
- **堅牢な入力処理**  
  空文字チェックを行い、無効な送信を防止。
- **モジュール連携の明確化**  
  `BrowserBlockEntity` → `BrowserSearchScreen` → `RequestUrlPacket` → サーバー処理 → `BrowserDisplayScreen` というデータフローを明確に分離。

---

## 💡 まとめ
`BrowserSearchScreen` は、Minecraft 内での「インターネットブラウジング体験」を模倣する第一段階であり、  
プレイヤー入力（URL指定）からサーバー通信、ローディング画面遷移までを統合的に制御する重要なインターフェースクラスである。  
このクラスにより、仮想ネットワーク構造（ルーターやDNSサーバー）を介した通信シミュレーションの基盤が形成されている。