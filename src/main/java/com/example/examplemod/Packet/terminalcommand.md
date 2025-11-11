# コードドキュメント：`TerminalCommandPacket.java`

## 🧩 概要
このクラス `TerminalCommandPacket` は、Minecraft Forge のネットワークパケットシステムを利用して、**クライアントのターミナル入力をサーバー側の `ServerBlockEntity` に送信し、コマンド実行結果をクライアントに返す**ための通信処理を実装している。

すなわち、「Minecraft 内での仮想サーバーターミナル（コンソール）」のような仕組みを実現するための、**クライアント → サーバー → クライアント** の双方向データ伝達を担うクラスである。

---

## 🧠 主な処理の流れ
1. クライアント側でターミナル入力（コマンド文字列）が行われる。
2. `TerminalCommandPacket` が生成され、対象のブロック位置 (`BlockPos`) とコマンド文字列がパケットとしてサーバーへ送信される。
3. サーバー側で `handle()` が呼び出され、対応する `ServerBlockEntity` を取得。
4. `ServerBlockEntity#executeCommand()` を通じてコマンドが実行される。
5. 実行結果の文字列を `sendTerminalOutputToClient()` を用いてクライアントへ返信。

---

## 🧾 グローバル変数一覧

| 変数名 | 型 | 初期値 | 使用箇所 | 説明 |
|--------|----|--------|----------|------|
| `pos` | `BlockPos` | コンストラクタで指定 | `encode()`, `handle()` | コマンド実行対象となる `ServerBlockEntity` の座標。 |
| `command` | `String` | コンストラクタで指定 | `encode()`, `handle()` | クライアントのターミナルから送信されたコマンド文字列。 |

---

## ⚙️ 関数一覧

### `TerminalCommandPacket(BlockPos pos, String command)`
- **概要**: コンストラクタ。パケットの送信内容（ブロック座標とコマンド文字列）を初期化。
- **引数**:
    - `pos`: コマンドを実行する対象ブロックの座標。
    - `command`: 実行するコマンドの文字列。
- **呼び出し元**: `decode()`
- **呼び出し先**: なし。

---

### `encode(TerminalCommandPacket packet, FriendlyByteBuf buf)`
- **戻り値**: なし (`void`)
- **概要**: パケットをサーバー送信前にバイトストリームへ変換（シリアライズ）する。
- **主な処理**:
    1. `writeBlockPos(packet.pos)` で対象ブロック座標を書き込み。
    2. `writeUtf(packet.command)` でコマンド文字列を書き込み。
- **呼び出し元**: Forge のネットワークシステム (`SimpleChannel#sendToServer`)
- **呼び出し先**: `FriendlyByteBuf` クラス（データバッファ）
- **目的**: パケットをサーバー側で再構築できるよう、シリアル化形式に変換する。

---

### `decode(FriendlyByteBuf buf) -> TerminalCommandPacket`
- **戻り値**: 新しい `TerminalCommandPacket` インスタンス
- **概要**: サーバーが受信したデータをデシリアライズ（復元）し、パケットオブジェクトに変換する。
- **主な処理**:
    1. `readBlockPos()` で座標を読み取る。
    2. `readUtf()` でコマンド文字列を取得。
    3. 新しい `TerminalCommandPacket` を生成して返す。
- **呼び出し元**: Forge ネットワークデコード処理 (`SimpleChannel#registerMessage`)

---

### `handle(TerminalCommandPacket packet, Supplier<NetworkEvent.Context> ctx)`
- **戻り値**: なし (`void`)
- **概要**: サーバー側でパケットを受信後、コマンド実行と結果返信を行うメイン処理。
- **引数**:
    - `packet`: デコード済みの `TerminalCommandPacket`
    - `ctx`: Forge の `NetworkEvent.Context`（非同期イベント制御）
- **主な処理フロー**:
    1. `ctx.get().enqueueWork()` でメインスレッドに処理を委譲（スレッドセーフに実行）。
    2. `ctx.get().getSender()` から送信者（`ServerPlayer`）を取得。
    3. `player == null` の場合は安全のため処理終了。
    4. `player.level.getBlockEntity(packet.pos)` で座標位置のブロックエンティティを取得。
    5. インスタンスが `ServerBlockEntity` でなければ終了。
    6. `serverEntity.executeCommand(packet.command)` を呼び出して、受信したコマンドをサーバー側で実行。
    7. 実行結果の文字列を `serverEntity.sendTerminalOutputToClient(player, result)` でクライアントへ返信。
    8. 最後に `ctx.get().setPacketHandled(true)` で処理完了を宣言。

- **呼び出し元**: Forge のネットワークパケットハンドラ。
- **呼び出し先**:
    - `ServerBlockEntity#executeCommand(String)`
    - `ServerBlockEntity#sendTerminalOutputToClient(ServerPlayer, String)`
- **エラーハンドリング**:
    - `player == null` の場合早期 return。
    - 指定座標のブロックが `ServerBlockEntity` でない場合も return。

---

## 🔗 呼び出し関係図
```
クライアント
└─ sendToServer(new TerminalCommandPacket(pos, command))
↓
サーバー
└─ TerminalCommandPacket.handle()
├─ ServerBlockEntity.executeCommand(command)
└─ ServerBlockEntity.sendTerminalOutputToClient(player, result)
```

---

## 🧩 外部依存関係

| クラス名 | パッケージ | 用途 |
|-----------|-------------|------|
| `FriendlyByteBuf` | `net.minecraft.network` | Forge ネットワーク通信でデータの読み書きを行うバッファクラス。 |
| `NetworkEvent.Context` | `net.minecraftforge.network` | パケットの受信処理をメインスレッドで安全に実行するための制御クラス。 |
| `ServerPlayer` | `net.minecraft.server.level` | サーバー上のプレイヤー情報。送信元プレイヤーの取得や返信時に使用。 |
| `BlockPos` | `net.minecraft.core` | ワールド内のブロック座標を表現。対象ブロック特定に利用。 |
| `ServerBlockEntity` | `com.example.examplemod.Blocks.ServerBlock` | 仮想サーバーブロックの実体。コマンド実行 (`executeCommand`) や出力送信 (`sendTerminalOutputToClient`) を担当。 |

---

## 📦 ファイル別概要

| ファイル名 | 主な責務 | 主な関数 |
|-------------|-----------|-----------|
| `TerminalCommandPacket.java` | クライアントのターミナル入力をサーバーに転送し、実行結果をクライアントへ返すためのネットワーク通信処理 | `encode()`, `decode()`, `handle()` |

---

## 🧭 処理全体の流れ（ストーリー形式）

1. プレイヤーが Minecraft 内のターミナルブロック（`ServerBlockEntity`）にコマンドを入力。
2. クライアントが `TerminalCommandPacket` を生成し、コマンドとブロック座標をサーバーに送信。
3. サーバーがパケットを受信し、`handle()` にて対象ブロックを検索。
4. ブロックがサーバーエンティティであれば `executeCommand()` を呼び出し、内部で仮想シェル処理を実行。
5. 実行結果（出力メッセージ）を `sendTerminalOutputToClient()` でクライアントへ返信。
6. クライアント側のターミナルUIに出力結果が表示され、まるで実際のサーバーコマンドのように動作する。

---

## ⚙️ 設計上のポイント
- **スレッドセーフ設計**
    - ネットワークイベントは別スレッドで処理されるため、`enqueueWork()` によりワールド操作をメインスレッドで実行する。
- **シンプルなデータ構造**
    - パケットは `BlockPos` と `String` のみを保持し、データ転送効率が高い。
- **モジュール的な拡張性**
    - `ServerBlockEntity#executeCommand()` の内部処理を拡張することで、複数のサーバー機能やコマンドシステムに対応可能。

---

## 🔍 まとめ
この `TerminalCommandPacket` クラスは、**Minecraft 内の仮想サーバー環境を実現する通信の心臓部**であり、クライアントから送信されたコマンドをサーバーが安全に実行し、その出力をリアルタイムでクライアントに返す仕組みを構築している。  
Minecraft の「ブロック × ネットワーク × インタラクション」を融合させた高度なデータ通信設計の一例である。
