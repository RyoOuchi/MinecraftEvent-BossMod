# コードドキュメント：`TerminalScreen.java`

## 🧩 概要
このクラス `TerminalScreen` は、Minecraft 内で動作する**仮想ターミナル（コンソール）UI**を実装している。  
プレイヤーがコマンドを入力し、`ServerBlockEntity` と通信してサーバー操作を行う仕組みを提供する。  
UNIX風の操作体系を再現し、`connect`, `help`, `clear`, `exit` などの基本コマンドをサポートしている。

主な処理の流れ：
1. GUIが初期化され、ターミナル出力ログ(`log`)と入力欄(`input`)がセットアップされる。
2. ユーザーが入力欄にコマンドを入力し、Enterキーを押す。
3. `executeCommand()`が呼ばれ、コマンド内容を解析・実行。
4. 結果は `log` に追加され、`render()` 内で描画。
5. 必要に応じて `TerminalCommandPacket` をサーバーへ送信し、リモート実行結果を受け取る。

---

## 🧾 グローバル変数一覧

| 変数名 | 型 | 初期値 | 使用箇所 | 説明 |
|--------|----|--------|----------|------|
| `parent` | `Screen` | コンストラクタ引数 | `executeCommand()`（`exit`コマンド） | ターミナル終了後に戻る親画面。 |
| `pos` | `BlockPos` | コンストラクタ引数 | `executeCommand()`, `connectToServer()` | 対応するブロック位置（ターミナルのある座標）。 |
| `blockEntity` | `BlockEntity` | コンストラクタ引数 | `executeCommand()`, `connectToServer()` | 実際のターミナルブロックのエンティティ情報。 |
| `log` | `List<String>` | `new ArrayList<>()` | `render()`, `executeCommand()`, `addLogLine()` | ターミナル出力のログ履歴。 |
| `input` | `EditBox` | `null` → `init()` で生成 | `render()`, `keyPressed()` | ユーザー入力欄。 |
| `scrollOffset` | `double` | `0` | `mouseScrolled()`, `updateScroll()` | 現在のスクロール位置。 |
| `visibleLines` | `int` | `14` | `render()` | 一度に表示できる行数。 |
| `maxScroll` | `double` | `0` | `updateScroll()`, `mouseScrolled()` | スクロール可能な最大行数。 |
| `connected` | `boolean` | `false` | `executeCommand()`, `connectToServer()` | サーバー接続状態フラグ。 |
| `currentPath` | `String` | `"/"` | `executeCommand()`, `render()` | 現在の作業ディレクトリを示す。 |
| `PROMPT_SUFFIX` | `String` | `" > "` | `render()`, `executeCommand()` | コマンドプロンプト末尾の表示文字列。 |

---

## ⚙️ 関数一覧

### `TerminalScreen(Screen parent, BlockPos pos, BlockEntity blockEntity)`
- **概要**: コンストラクタ。ターミナル画面を初期化し、ブロック位置とエンティティを保持。
- **引数**:
    - `parent`: 前の画面（戻り先）。
    - `pos`: ターミナルブロックの位置。
    - `blockEntity`: このターミナルの `BlockEntity`。
- **呼び出し元**: `ServerBlock` や他の UI クラスから呼び出される。

---

### `init()`
- **戻り値**: なし (`void`)
- **概要**: GUIの初期化。入力欄(`EditBox`)とカーソル点滅処理を設定し、初期メッセージを追加。
- **主な処理**:
    1. 入力欄 (`EditBox`) を画面下部に生成。
    2. 独自の `renderButton()` をオーバーライドし、点滅カーソルを再現。
    3. `setBordered(false)` により枠線を非表示。
    4. `log` に `"Terminal ready..."` を追加。
- **呼び出し元**: 画面初期化時（Forge 自動呼び出し）。
- **呼び出し先**: なし。

---

### `executeCommand(String cmd)`
- **戻り値**: なし (`void`)
- **概要**: ユーザーが入力したコマンド文字列を解析し、対応する処理を実行。
- **主な処理**:
    1. コマンドを `log` に追記。
    2. コマンドが空白なら終了。
    3. `switch` 文でコマンドを処理：
        - `exit`: 親画面へ戻る。
        - `help`:
            - 未接続時 → ローカルヘルプ表示。
            - 接続時 → `TerminalCommandPacket("help")` を送信。
        - `clear`: ログを全削除。
        - `connect`: `connectToServer()` を呼び出し。
        - その他：
            - サーバー接続済みなら `TerminalCommandPacket(cmd)` を送信。
            - 未接続なら「Not connected」警告を表示。
    4. `updateScroll()` でスクロール位置更新。
- **呼び出し元**: `keyPressed()`（Enter押下時）。
- **呼び出し先**:
    - `connectToServer()`
    - `updateScroll()`
    - `ExampleMod.CHANNEL.sendToServer(new TerminalCommandPacket(...))`
- **エラーハンドリング**:
    - `blockEntity.getLevel()` が `null` の場合は警告を出力。

---

### `updateScroll()`
- **概要**: ログ行数に応じてスクロール最大値を更新。
- **処理**:
    1. `maxScroll = max(0, log.size() - visibleLines)`
    2. `scrollOffset = maxScroll`（最新行が常に表示されるように設定）
- **呼び出し元**: `executeCommand()`, `addLogLine()`

---

### `addLogLine(String line)`
- **概要**: 新しい行をログに追加。
- **処理**: 空行を除外し `log` に追加、`updateScroll()` を呼び出す。
- **呼び出し元**: `TerminalOutputPacket.handleClient()`（サーバーからの出力受信時）。

---

### `setPromptPath(String newPath)`
- **概要**: コマンドプロンプトの表示パスを更新。
- **呼び出し元**: サーバー側出力によりパスが変更された際（例: `cd` コマンド実行後）。

---

### `connectToServer()`
- **概要**: ターミナル下のブロックに存在する `ServerBlockEntity` へ接続を試みる。
- **処理**:
    1. ログに「Connecting...」メッセージを追加。
    2. `blockEntity.getLevel()` が `null` の場合はエラーを出力。
    3. `pos.below()` のブロックが `ServerBlockEntity` であれば接続成功。
    4. 接続成功時、`connected = true` に設定。
    5. 失敗時、「No server block found below.」を出力。
- **呼び出し元**: `executeCommand("connect")`。
- **呼び出し先**: `setPromptPath("/")`。

---

### `keyPressed(int keyCode, int scanCode, int modifiers) -> boolean`
- **概要**: キー入力イベントの処理。Enterキー（コード257）が押されたとき、入力欄の内容を `executeCommand()` に渡す。
- **処理**:
    - 入力が空でなければ実行。
    - 実行後、入力欄をクリア。
- **呼び出し元**: Minecraft の入力イベント。

---

### `mouseScrolled(double mouseX, double mouseY, double delta) -> boolean`
- **概要**: スクロールホイールによるログの上下移動を処理。
- **処理**:
    - `scrollOffset` を `delta` に応じて増減。
    - 範囲外スクロールを制限。
- **呼び出し元**: Minecraft のマウスイベント。

---

### `render(PoseStack stack, int mouseX, int mouseY, float partialTicks)`
- **概要**: ターミナル画面の描画。ログ、プロンプト、入力欄を整列して描画する。
- **主な処理**:
    1. 背景描画。
    2. `log` のうち `scrollOffset` に応じた範囲のみ表示。
    3. `(base) / > ` のようなプロンプトを描画。
    4. 入力欄 (`EditBox`) をその右側に配置して描画。
- **呼び出し元**: Minecraft のフレームレンダリングループ。

---

### `isPauseScreen() -> boolean`
- **戻り値**: `false`
- **概要**: この画面が開いていてもゲームを一時停止しないようにする設定。

---

## 🔗 呼び出し関係図
```
init()
├─ log.add(“Terminal ready…”)
└─ addRenderableWidget(input)

keyPressed()
└─ executeCommand(command)
├─ connectToServer()
│    ├─ blockEntity.getLevel()
│    └─ setPromptPath(”/”)
├─ updateScroll()
└─ ExampleMod.CHANNEL.sendToServer(new TerminalCommandPacket(…))

TerminalOutputPacket.handleClient()
└─ addLogLine()
└─ updateScroll()

render()
├─ draw logs from log[]
├─ draw prompt (currentPath + PROMPT_SUFFIX)
└─ input.render()
```

---

## 🧩 外部依存関係

| クラス | パッケージ | 用途 |
|--------|-------------|------|
| `Screen` | `net.minecraft.client.gui.screens` | Minecraft の画面管理クラス。 |
| `EditBox` | `net.minecraft.client.gui.components` | コマンド入力欄の実装。 |
| `TerminalCommandPacket` | `com.example.examplemod.Packet` | コマンドをサーバーへ送信するパケット。 |
| `ServerBlockEntity` | `com.example.examplemod.Blocks.ServerBlock` | サーバーブロックエンティティ。接続確認に使用。 |
| `ExampleMod.CHANNEL` | `com.example.examplemod` | Forge ネットワーク通信チャンネル。 |
| `PoseStack` | `com.mojang.blaze3d.vertex` | 描画行列制御。 |
| `TextComponent` | `net.minecraft.network.chat` | テキストラベルの描画に使用。 |

---

## 📦 ファイル別概要

| ファイル名 | 主な責務 | 主な関数 |
|-------------|-----------|-----------|
| `TerminalScreen.java` | Minecraft 内での仮想ターミナル（コマンド実行UI）を実装し、サーバー通信やログ管理を行う。 | `executeCommand()`, `connectToServer()`, `render()`, `addLogLine()` |

---

## 🧭 処理全体の流れ（ストーリー形式）

1. プレイヤーがVSCodeブロックを操作して `TerminalScreen` を開く。
2. 画面初期化時にログメッセージ「Terminal ready...」が表示される。
3. プレイヤーがコマンドを入力して Enter を押すと、`executeCommand()` が呼ばれる。
4. コマンドが `connect` の場合、下方向のブロック（`pos.below()`）を探索し、サーバーが存在すれば接続成功。
5. 接続中の状態では、入力コマンドは `TerminalCommandPacket` としてサーバーに送信される。
6. サーバーからの応答は `TerminalOutputPacket` によりクライアントの `addLogLine()` に反映される。
7. ログが増えるたびにスクロール位置が更新され、最新行が常に表示される。
8. `exit` コマンドで親画面に戻る。

---

## ⚙️ 設計上の特徴

- **UNIX風ターミナル再現**  
  コマンド入力・プロンプト表示・接続状態・スクロール機能を組み合わせて、リアルなCLI体験を実装。
- **リアルタイム通信**  
  `ExampleMod.CHANNEL` を介してサーバーと即時コマンド通信を行う。
- **自動スクロール管理**  
  `updateScroll()` により新規出力行が常に可視範囲に入るよう制御。
- **視覚的フィードバック**  
  カーソル点滅やカラープロンプトにより、ターミナルらしさを演出。
- **拡張性**  
  将来的にサーバーコマンドの追加（例：`ls`, `cd`, `mkdir`）が容易な構成。

---

## 💡 まとめ
`TerminalScreen` は、Minecraft 内で「仮想サーバー操作」を行うための主要インターフェースであり、  
ユーザー入力、サーバー通信、出力ログ描画を一貫して管理する中核コンポーネントである。  
このクラスにより、ブロックエンティティとネットワーク通信を介したコマンド実行の擬似シェル環境が実現されている。