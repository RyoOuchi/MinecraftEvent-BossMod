# コードドキュメント：`RequestUrlPacket.java`

## 🧩 概要
このクラス `RequestUrlPacket` は、Minecraft Forge のネットワーク通信（Packet Handling）を通じて、クライアント側からサーバー側へ「URLリクエスト」を送信し、対応する `BrowserBlockEntity` と `WifiRouterBlockEntity` を操作するためのパケット定義クラスである。  
主な流れは以下の通り：

1. クライアントがURL（`byte[] urlByte`）とブロック座標（`BlockPos pos`）を含む `RequestUrlPacket` を作成し送信する。
2. サーバー側で `handle()` が呼び出され、受信したパケットを処理する。
3. 対象座標付近に存在する最も近い `WifiRouterBlockEntity` を探索し、`performDNSRequest()` を実行する。
4. 対象の `BrowserBlockEntity` にURLデータをセットする。

このように、ブラウザブロックとWi-Fiルーターブロック間で「仮想インターネット通信」を模擬する仕組みの一部を担っている。

---

## 🧠 グローバル変数一覧

| 変数名 | 型 | 初期値 | 使用箇所 | 説明 |
|--------|----|--------|----------|------|
| `urlByte` | `byte[]` | コンストラクタで受け取り | `encode()`, `handle()` | クライアントから送られるURLデータをバイト配列で保持する。`BrowserBlockEntity`に渡される。 |
| `pos` | `BlockPos` | コンストラクタで受け取り | `encode()`, `handle()` | ブロック（ブラウザ）の位置を示すMinecraft座標。周囲のルーター探索や対象ブロック特定に利用される。 |

---

## ⚙️ 関数一覧

### ### `RequestUrlPacket(byte[] urlByte, BlockPos pos)`
- **概要**: コンストラクタ。クライアントから受け取ったURLバイトデータとブロック座標を格納する。
- **引数**:
    - `urlByte`: URLをバイト列として表したもの。
    - `pos`: 対象のブロック座標。
- **呼び出し元**: `decode()`
- **役割**: パケットの内容をインスタンスに格納する。

---

### `encode(RequestUrlPacket msg, FriendlyByteBuf buf)`
- **戻り値**: なし（`void`）
- **概要**: パケット送信前に、データをバイトバッファへシリアライズ（直列化）する。
- **主な処理**:
    1. `msg.urlByte` を `buf.writeByteArray()` で書き込む。
    2. `msg.pos` を `buf.writeBlockPos()` で書き込む。
- **呼び出し元**: Forgeのネットワーク送信処理 (`SimpleChannel#sendToServer`)
- **役割**: クライアントからサーバーに送るためのデータ変換。

---

### `decode(FriendlyByteBuf buf) -> RequestUrlPacket`
- **戻り値**: `RequestUrlPacket` インスタンス
- **概要**: サーバー受信時に、バイトストリームから `RequestUrlPacket` オブジェクトを復元する。
- **主な処理**:
    1. `buf.readByteArray()` でURLデータを読み取る。
    2. `buf.readBlockPos()` でブロック座標を取得。
    3. 新しい `RequestUrlPacket` インスタンスを生成して返す。
- **呼び出し元**: Forgeのデコードルーチン（`SimpleChannel#registerMessage`登録時）

---

### `handle(RequestUrlPacket msg, Supplier<NetworkEvent.Context> ctx)`
- **戻り値**: なし（`void`）
- **概要**: 受信したパケットをサーバー側で処理するメイン関数。
- **引数**:
    - `msg`: デコード済みの `RequestUrlPacket`
    - `ctx`: `NetworkEvent.Context` サプライヤ（非同期処理コンテキスト）
- **呼び出し元**: Forgeのネットワークハンドラ
- **主な処理フロー**:

    1. `ctx.get().enqueueWork()` によりメインスレッドで安全に処理。
    2. `ServerPlayer` オブジェクトを取得（送信者）。
    3. `player.sendMessage()` でサーバー側処理開始の通知を送信。
    4. 指定座標を中心とした5ブロック範囲のAABB（立方体領域）を生成。
    5. `WifiRouterBlockEntity` を探索：
        - 範囲内すべての `BlockPos` を走査。
        - 最も近い距離のルーターを選択。
    6. ルーターが見つかれば：
        - `player.sendMessage()` で位置を通知。
        - `closestRouter.performDNSRequest(msg.urlByte, null, msg.pos)` を実行。  
          （URLバイトデータをDNSリクエストとして処理する想定。）
    7. 見つからない場合：
        - 「No WifiRouter found nearby!」と通知。
    8. `msg.pos` にある `BlockEntity` を確認し、`BrowserBlockEntity` なら：
        - `browserBlockEntity.setUrlData(msg.urlByte)` でURLデータをセット。
    9. 最後に `ctx.get().setPacketHandled(true)` で完了を通知。

- **呼び出し先**:
    - `WifiRouterBlockEntity#performDNSRequest(byte[], ?, BlockPos)`
    - `BrowserBlockEntity#setUrlData(byte[])`
    - `ServerPlayer#sendMessage(TextComponent, UUID)`

- **エラー・例外処理**:
    - `player == null` の場合は即座に `return`。
    - `BlockEntity` が `BrowserBlockEntity` でない場合は何もせず終了。

---

## 🔗 呼び出し関係図
```
クライアント
└─ sendToServer(new RequestUrlPacket(url, pos))
↓
サーバー側
└─ RequestUrlPacket.handle()
├─ WifiRouterBlockEntity.performDNSRequest(urlByte, null, pos)
└─ BrowserBlockEntity.setUrlData(urlByte)
```

---

## 🧩 外部依存関係

| クラス名 | パッケージ | 用途 |
|-----------|-------------|------|
| `FriendlyByteBuf` | `net.minecraft.network` | パケットデータのシリアライズ／デシリアライズ用 |
| `NetworkEvent.Context` | `net.minecraftforge.network` | Forgeの非同期ネットワークイベント管理 |
| `ServerPlayer` | `net.minecraft.server.level` | サーバー上のプレイヤー情報を操作 |
| `BlockEntity`, `BlockPos` | `net.minecraft.world.level.block.entity`, `net.minecraft.core` | Minecraftのブロック位置・状態の管理 |
| `WifiRouterBlockEntity` | `com.example.examplemod.Blocks.WifiRouterBlock` | 仮想Wi-Fiルーターを表すカスタムブロックエンティティ |
| `BrowserBlockEntity` | `com.example.examplemod.Blocks.BrowserBlock` | 仮想ブラウザブロック。URLデータを受け取る。 |

---

## 📦 ファイル別概要

| ファイル名 | 主な責務 | 主な関数 |
|-------------|-----------|-----------|
| `RequestUrlPacket.java` | クライアントからサーバーへのURLリクエストを定義し、ブラウザブロックとWi-Fiルーターブロックを仲介するネットワークパケット処理 | `encode()`, `decode()`, `handle()` |

---

## 🧭 処理全体の流れ（ストーリー的説明）

1. プレイヤーがブラウザブロックでURLを入力すると、URLデータが `byte[]` に変換され、`RequestUrlPacket` が生成される。
2. このパケットがサーバーへ送信されると、サーバーは `handle()` メソッドを通じて受信を処理。
3. サーバーはパケットの座標を中心に近隣の `WifiRouterBlockEntity` を検索し、最も近いルーターを見つける。
4. 見つかったルーターに対し、`performDNSRequest()` を呼び出し、仮想的なDNS問い合わせを行う。
5. 同時に、対応する `BrowserBlockEntity` にURLデータをセットしてブラウザの表示を更新する。
6. 最後に、プレイヤーに対して処理完了やエラーメッセージをサーバーチャットで送信する。

---

このコードは、**Minecraft内でのインターネット通信シミュレーション**の中核的部分であり、実際のネットワーク通信を模倣したリアルなデータ伝送フローをForgeパケットシステムを通じて実現している。