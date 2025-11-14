# コードドキュメント：`TerminalOutputPacket.java`

## 🧩 概要
このクラス `TerminalOutputPacket` は、**サーバー側で実行されたターミナルコマンドの出力結果をクライアント側の画面 (`TerminalScreen`) に表示するためのネットワークパケット処理**を実装している。

Minecraft Forge のネットワーク通信システムを用い、サーバーからクライアントへ文字列データ（出力ログおよびカレントディレクトリ）を安全に転送する役割を持つ。  
これにより、Minecraft 内の仮想ターミナルで「サーバーコマンドを実行し、その出力をリアルタイムに表示する」動作を実現している。

---

## ⚙️ 主な処理の流れ
1. サーバー側でコマンドが実行され、その出力と現在のパス (`currentPath`) が生成される。
2. サーバーが `TerminalOutputPacket` を生成し、クライアントへ送信する。
3. クライアント側の `handle()` が呼ばれ、メインスレッド上で `handleClient()` を実行。
4. `Minecraft` インスタンスから現在の画面を取得し、それが `TerminalScreen` の場合のみ、出力行を UI に追加。
5. `setPromptPath()` により、ターミナルのカレントディレクトリを更新。

---

## 🧾 グローバル変数一覧

| 変数名 | 型 | 初期値 | 使用箇所 | 説明 |
|--------|----|--------|----------|------|
| `output` | `String` | コンストラクタで指定 | `encode()`, `handleClient()` | サーバーで実行されたコマンドの出力文字列。複数行の場合は改行で区切られる。 |
| `currentPath` | `String` | コンストラクタで指定 | `encode()`, `handleClient()` | ターミナルの現在の作業ディレクトリを示すパス。 |

---

## 🧩 関数一覧

### `TerminalOutputPacket(String output, String currentPath)`
- **概要**: コンストラクタ。出力結果文字列と現在のパスを初期化。
- **引数**:
    - `output`: サーバー側の実行結果を格納した文字列。
    - `currentPath`: クライアントのターミナルで表示すべき現在のディレクトリパス。
- **呼び出し元**: `decode()`、およびサーバー側パケット送信コード。
- **呼び出し先**: なし。

---

### `encode(TerminalOutputPacket packet, FriendlyByteBuf buf)`
- **戻り値**: なし (`void`)
- **概要**: パケット送信前に、データをシリアライズ（直列化）して `FriendlyByteBuf` に書き込む。
- **主な処理**:
    1. `writeUtf(packet.output)` で出力内容をUTF-8文字列として書き込む。
    2. `writeUtf(packet.currentPath)` でカレントディレクトリのパスを追加書き込み。
- **呼び出し元**: Forge ネットワーク送信ルーチン (`SimpleChannel#send()`)。
- **呼び出し先**: `FriendlyByteBuf` の書き込みAPI。

---

### `decode(FriendlyByteBuf buf) -> TerminalOutputPacket`
- **戻り値**: 新しい `TerminalOutputPacket` インスタンス。
- **概要**: サーバーから受信したバイト列を復元（デシリアライズ）して、パケットオブジェクトを生成。
- **主な処理**:
    1. `buf.readUtf()` で出力文字列を読み取る。
    2. `buf.readUtf()` でカレントパスを読み取る。
    3. 新しい `TerminalOutputPacket` を返す。
- **呼び出し元**: Forge の受信デコード処理 (`SimpleChannel#registerMessage`)。
- **呼び出し先**: なし。

---

### `handle(TerminalOutputPacket packet, Supplier<NetworkEvent.Context> ctx)`
- **戻り値**: なし (`void`)
- **概要**: クライアント側でパケットを受け取った際に呼び出されるメインハンドラ。
- **主な処理**:
    1. `ctx.get().enqueueWork()` を用いて、Minecraft のメインスレッド上で安全にクライアント処理を実行。
    2. `handleClient(packet)` を呼び出して実際のUI更新を行う。
    3. 最後に `setPacketHandled(true)` を呼び出して処理完了を宣言。
- **呼び出し元**: Forge ネットワークイベントシステム。
- **呼び出し先**: `handleClient()`

---

### `@OnlyIn(Dist.CLIENT) handleClient(TerminalOutputPacket msg)`
- **戻り値**: なし (`void`)
- **概要**: クライアント側限定で実行される、ターミナル画面 (`TerminalScreen`) の更新処理。
- **主な処理フロー**:
    1. `Minecraft.getInstance()` によりクライアント環境の `Minecraft` オブジェクトを取得。
    2. 現在のスクリーンが `TerminalScreen` であるかを確認。
    3. `msg.output` を改行ごとに分割し、各行を `terminal.addLogLine(line)` でターミナルログに追加。
    4. `terminal.setPromptPath(msg.currentPath)` を呼び出して、現在のパスを更新。
- **呼び出し元**: `handle()` メソッド内 (`enqueueWork()` 経由)。
- **呼び出し先**:
    - `TerminalScreen#addLogLine(String)`
    - `TerminalScreen#setPromptPath(String)`
- **エラーハンドリング**:
    - `Minecraft.getInstance()` または `mc.screen` が `TerminalScreen` でない場合は何も行わず終了。

---

## 🔗 呼び出し関係図
```
サーバー
└─ sendToClient(new TerminalOutputPacket(output, currentPath))
↓
クライアント
└─ TerminalOutputPacket.handle()
└─ handleClient()
├─ TerminalScreen.addLogLine()
└─ TerminalScreen.setPromptPath()
```
---

## 🧩 外部依存関係

| クラス名 | パッケージ | 用途 |
|-----------|-------------|------|
| `FriendlyByteBuf` | `net.minecraft.network` | パケットのデータをシリアライズ／デシリアライズするためのバッファ。 |
| `NetworkEvent.Context` | `net.minecraftforge.network` | パケット受信時のイベント情報を保持。メインスレッド実行を制御。 |
| `Minecraft` | `net.minecraft.client` | 現在のクライアントインスタンスおよびアクティブ画面の取得。 |
| `TerminalScreen` | `com.example.examplemod.Screens.TerminalScreen` | カスタムターミナルUIクラス。ログの追加・パス更新を行う。 |
| `@OnlyIn(Dist.CLIENT)` | `net.minecraftforge.api.distmarker` | このメソッドがクライアント環境専用であることを明示。 |

---

## 📦 ファイル別概要

| ファイル名 | 主な責務 | 主な関数 |
|-------------|-----------|-----------|
| `TerminalOutputPacket.java` | サーバーで生成されたコマンド出力をクライアント側に送り、ターミナル画面 (`TerminalScreen`) に表示する。 | `encode()`, `decode()`, `handle()`, `handleClient()` |

---

## 🧭 処理全体の流れ（ストーリー形式）

1. サーバーで `ServerBlockEntity` がコマンドを実行し、結果の出力文字列 (`output`) と現在の作業パス (`currentPath`) を生成。
2. サーバーがこれらを含む `TerminalOutputPacket` を作成し、対象プレイヤーへ送信。
3. クライアント側で `handle()` が呼び出され、`enqueueWork()` により安全にメインスレッドで処理を行う。
4. クライアントの現在の画面が `TerminalScreen` であれば、出力ログを画面に1行ずつ追加 (`addLogLine()`)。
5. さらに、コマンド実行後のカレントディレクトリを `setPromptPath()` で更新。
6. 結果として、プレイヤーはターミナルUI上で「サーバーコマンドの結果」をリアルタイムで確認できる。

---

## ⚙️ 設計上の特徴
- **明確なクライアント専用処理**  
  `@OnlyIn(Dist.CLIENT)` アノテーションにより、クライアント以外（サーバー環境）で誤って呼び出されることを防止している。
- **スレッドセーフな設計**  
  Forge の `enqueueWork()` を使用することで、Minecraft のメインスレッドに安全にUI更新を委譲している。
- **UIとの密接な連携**  
  出力ログとパス情報が `TerminalScreen` に直接反映され、ターミナル型のインタラクション体験を提供している。

---

## 💡 まとめ
`TerminalOutputPacket` は、**サーバー → クライアント間の一方向通信を担う出力専用パケットクラス**であり、  
仮想ターミナルシステムにおける「コマンド結果のリアルタイム表示」を実現する中核部分である。  
このクラスにより、Minecraft 内でのネットワークシミュレーションにおけるユーザー体験が、より現実のコンソール操作に近づけられている。