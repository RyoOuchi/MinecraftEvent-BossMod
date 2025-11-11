# コードドキュメント：`ServerDomainScreen.java`

## 🧩 概要
このクラス `ServerDomainScreen` は、Minecraft 内の**仮想サーバー管理機能**において、プレイヤーが新しいサーバードメインを登録するための入力画面を提供する。  
ユーザーがテキスト入力欄にドメイン名を入力し「Add Server」ボタンを押すと、その情報を `AddServerPacket` としてサーバーに送信する。  
この画面は、サーバーブロック (`ServerBlockEntity`) に紐づく登録処理のクライアント側インターフェースとして機能している。

主な処理の流れ：
1. 画面が初期化される (`init()`)。
2. ユーザーがドメイン名を入力。
3. 「Add Server」ボタンを押すと `onConfirmPressed()` が実行され、
    - 入力内容を検証 →
    - サーバーへ `AddServerPacket` を送信 →
    - 画面を閉じる。

---

## 🧾 グローバル変数一覧

| 変数名 | 型 | 初期値 | 使用箇所 | 説明 |
|--------|----|--------|----------|------|
| `domainInput` | `EditBox` | `null`（初期値） | `init()`, `render()`, `onConfirmPressed()` | ドメイン名を入力するテキストボックス。最大64文字。 |
| `confirmButton` | `Button` | `null`（初期値） | `init()` | 「Add Server」ボタン。押下時に `onConfirmPressed()` を呼ぶ。 |
| `serverBlockPos` | `BlockPos` | コンストラクタ引数で設定 | `onConfirmPressed()` | 対象サーバーブロックのワールド座標。サーバー側で対応するエンティティを特定するために使用。 |

---

## ⚙️ 関数一覧

### `ServerDomainScreen(BlockPos serverBlockPos)`
- **概要**: コンストラクタ。指定されたサーバーブロックの位置を保持し、タイトルを `"Add Server"` に設定。
- **引数**:
    - `serverBlockPos`: サーバーブロックの座標。サーバー登録要求時にパケットへ付与される。
- **呼び出し元**: サーバーブロック (`ServerBlock`) の右クリック操作など、GUIを開くトリガーから呼ばれる。
- **呼び出し先**: `Screen` クラスのスーパークラスコンストラクタ。

---

### `init()`
- **戻り値**: なし (`void`)
- **概要**: GUIコンポーネント（入力欄とボタン）を初期化・配置する。
- **主な処理**:
    1. `centerX`, `centerY` を計算して画面中央座標を求める。
    2. `EditBox`（ドメイン入力欄）を生成し、中央に配置。
        - 幅200px、高さ20px、最大64文字入力可能。
        - 初期値は空文字。
    3. 「Add Server」ボタン (`Button`) を生成。押下時に `onConfirmPressed()` を実行。
    4. 両コンポーネントを `addRenderableWidget()` で画面に登録。
- **呼び出し元**: Forge により GUI が開かれた際に自動的に呼ばれる。
- **呼び出し先**: `onConfirmPressed()`（ボタン押下時）。

---

### `onConfirmPressed()`
- **戻り値**: なし (`void`)
- **概要**: 「Add Server」ボタン押下時に呼ばれる処理。入力されたドメイン名をサーバーに送信する。
- **主な処理**:
    1. 入力値取得: `domainInput.getValue().trim()`
    2. 空文字チェック：
        - 空の場合は `"⚠️ No domain entered!"` を `System.out.println` に出力し終了。
    3. 入力が有効な場合：
        - `"✅ Added server domain: ..."` をコンソールに出力。
        - `ExampleMod.CHANNEL.sendToServer()` を使い、`AddServerPacket` を送信。
            - パケット内容：`(enteredDomain, serverBlockPos)`
    4. 送信後、画面を閉じる (`this.minecraft.setScreen(null)`)。
- **呼び出し元**: `confirmButton` のクリックイベント。
- **呼び出し先**:
    - `ExampleMod.CHANNEL.sendToServer(new AddServerPacket(...))`
    - `Minecraft.setScreen(null)`
- **エラーハンドリング**:
    - 空文字時はサーバー送信を行わずログ出力のみ。

---

### `render(PoseStack poseStack, int mouseX, int mouseY, float partialTick)`
- **戻り値**: なし (`void`)
- **概要**: 毎フレーム呼ばれる描画処理。背景・タイトル・ウィジェットを描画する。
- **主な処理**:
    1. `renderBackground(poseStack)` で背景を描画。
    2. `"Enter Server Domain"` の文字を中央上部に描画。
    3. `super.render()` を呼び出してボタン描画処理を行う。
    4. `domainInput.render()` により入力欄を描画。
- **呼び出し元**: Minecraft のレンダリングループ。
- **呼び出し先**: `EditBox#render()`, `drawCenteredString()`。

---

### `isPauseScreen() -> boolean`
- **戻り値**: `false`
- **概要**: この画面を開いている間もゲームが一時停止しないようにする設定。
- **呼び出し元**: Minecraft の画面制御システム。

---

## 🔗 呼び出し関係図
```
ServerBlock
└─ new ServerDomainScreen(serverBlockPos)
    ├─ init()
    │   ├─ addRenderableWidget(domainInput)
    │   └─ addRenderableWidget(confirmButton)
    │         └─ onConfirmPressed()
    │               ├─ ExampleMod.CHANNEL.sendToServer(new AddServerPacket(…))
    │               └─ Minecraft.setScreen(null)
    ├─ render()
    └─ isPauseScreen()
```
---

## 🧩 外部依存関係

| クラス名 | パッケージ | 用途 |
|-----------|-------------|------|
| `Screen` | `net.minecraft.client.gui.screens` | Minecraft の GUI ベースクラス。 |
| `EditBox` | `net.minecraft.client.gui.components` | ユーザー文字列入力欄。 |
| `Button` | `net.minecraft.client.gui.components` | クリック可能ボタン。 |
| `PoseStack` | `com.mojang.blaze3d.vertex` | 描画用スタック。 |
| `BlockPos` | `net.minecraft.core` | ワールド上のブロック座標を表すクラス。 |
| `AddServerPacket` | `com.example.examplemod.Packet` | 新しいサーバーを登録するためのパケット。 |
| `ExampleMod.CHANNEL` | `com.example.examplemod` | Forge の通信チャンネル。サーバーとのデータ送受信に使用。 |
| `TextComponent` | `net.minecraft.network.chat` | 画面タイトルやボタンラベルの文字表示。 |

---

## 📦 ファイル別概要

| ファイル名 | 主な責務 | 主な関数 |
|-------------|-----------|-----------|
| `ServerDomainScreen.java` | サーバードメインの登録入力画面を提供。ユーザーが入力したドメインをサーバーに送信。 | `init()`, `onConfirmPressed()`, `render()` |

---

## 🧭 処理全体の流れ（ストーリー形式）

1. プレイヤーがサーバーブロックを右クリックすると `ServerDomainScreen` が開く。
2. 画面中央にドメイン入力欄と「Add Server」ボタンが表示される。
3. プレイヤーが入力欄に文字を入力。
4. 「Add Server」をクリックすると：
    - 入力内容が空でない場合、  
      a. ログにドメインを出力。  
      b. `AddServerPacket` に入力値とブロック位置を詰め、サーバーに送信。  
      c. 画面を閉じて通常表示に戻る。
    - 空の場合は警告をコンソールに表示し、処理を中断。
5. サーバー側では `AddServerPacket` を受信し、指定されたドメイン情報を登録する。

---

## ⚙️ 設計上の特徴

- **UIのシンプルさ**  
  入力欄と1つのボタンのみで構成されたミニマルな設計。
- **リアルタイム通信**  
  入力完了後すぐにパケットを送信し、サーバーと同期する非同期処理モデル。
- **安全な入力処理**  
  空文字チェックにより無効なリクエストを防止。
- **コンポーネント分離**  
  入力画面とネットワーク処理を明確に分離し、責務の単一化を実現。

---

## 💡 まとめ
`ServerDomainScreen` は、Minecraft 内の「仮想ネットワーク構築シミュレーション」における**ドメイン登録画面**であり、  
ユーザー操作からパケット通信を経てサーバーエンティティにドメインを追加するまでのフロントエンド処理を担う。  
本クラスを通して、ゲーム内で「ドメインとサーバーの紐づけ」というネットワーク的概念を直感的に体験できるよう設計されている。