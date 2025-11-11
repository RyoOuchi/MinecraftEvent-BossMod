# コードドキュメント：`SaveCodePacket.java`

## 🧩 概要
このクラス `SaveCodePacket` は、Minecraft Forge のネットワーク通信を通じて、クライアント側で編集されたコード（テキストデータ）をサーバー側へ送信し、`VSCodeBlockEntity` に保存するためのパケットクラスである。  
主に「Minecraft 内で動作する仮想 VSCode ブロック」における **ファイル保存処理** を担当している。

---

### 🧠 主な処理の流れ
1. クライアントでユーザーが VSCode ブロックのエディタに入力したコードを送信。
2. サーバーが `SaveCodePacket` を受信し、`handle()` メソッドを実行。
3. サーバーは `VSCodeBlockEntity` を座標から取得し、ブロックに挿入されているアイテムを `getItemInSlot()` で取得。
4. 取得した `ItemStack` に対して、`VSCodeBlockScreen.setCode()` を呼び出し、ファイル名とコードを保存する。
5. パケット処理を終了 (`setPacketHandled(true)`)。

---

## 🧾 グローバル変数一覧

| 変数名 | 型 | 初期値 | 使用箇所 | 説明 |
|--------|----|--------|----------|------|
| `fileName` | `String` | コンストラクタで指定 | `encode()`, `handle()` | 保存対象のファイル名を表す。 |
| `code` | `String` | コンストラクタで指定 | `encode()`, `handle()` | プレイヤーが入力したソースコードの文字列データ。 |
| `position` | `BlockPos` | コンストラクタで指定 | `encode()`, `handle()` | VSCode ブロックのワールド内の位置座標。対象ブロックを特定するために使用。 |

---

## ⚙️ 関数一覧

### `SaveCodePacket(String fileName, String code, BlockPos position)`
- **概要**: コンストラクタ。ファイル名・コード内容・ブロック座標を初期化。
- **引数**:
    - `fileName`: 保存するファイル名。
    - `code`: 実際のコードテキスト。
    - `position`: 対象のブロック位置（`BlockPos` 型）。
- **呼び出し元**: `decode()` メソッド。
- **呼び出し先**: なし。

---

### `encode(SaveCodePacket msg, FriendlyByteBuf buf)`
- **戻り値**: なし (`void`)
- **概要**: クライアントからサーバーへ送信する前に、パケットデータをシリアライズ（直列化）する処理。
- **主な処理**:
    1. `msg.fileName` を `writeUtf()` で書き込む。
    2. `msg.code` を `writeUtf()` で書き込む。
    3. `msg.position` を `writeBlockPos()` で書き込む。
- **呼び出し元**: Forge ネットワークシステム（`SimpleChannel#sendToServer()` など）。
- **呼び出し先**: `FriendlyByteBuf` の各書き込み関数。

---

### `decode(FriendlyByteBuf buf) -> SaveCodePacket`
- **戻り値**: 新しい `SaveCodePacket` インスタンス
- **概要**: サーバー受信時に、バイト列を読み取り `SaveCodePacket` オブジェクトとして復元。
- **主な処理**:
    1. `readUtf()` でファイル名を取得。
    2. `readUtf()` でコード文字列を取得。
    3. `readBlockPos()` でブロック位置を読み取る。
    4. 新しい `SaveCodePacket` を生成して返す。
- **呼び出し元**: Forge デコードプロセス（`SimpleChannel#registerMessage`）。

---

### `handle(SaveCodePacket msg, Supplier<NetworkEvent.Context> ctx)`
- **戻り値**: なし (`void`)
- **概要**: 受信した `SaveCodePacket` をサーバー側で処理し、コードをブロック内のアイテムへ保存する。
- **引数**:
    - `msg`: デコード済みの `SaveCodePacket` インスタンス。
    - `ctx`: Forge の `NetworkEvent.Context`（非同期処理を管理）。
- **主な処理**:
    1. `ctx.get().enqueueWork()` を使いメインスレッドで安全に実行。
    2. デバッグログとして受信内容（ファイル名・コード）を `System.out.println()`。
    3. 送信者（`ServerPlayer`）を `ctx.get().getSender()` から取得。
    4. 指定座標から `VSCodeBlockEntity` を取得。
        - `player.level.getBlockEntity(msg.position)`
    5. `null` チェックを行い、存在しなければ中断。
    6. ブロック内スロットのアイテムを `getItemInSlot()` で取得。
    7. `VSCodeBlockScreen.setCode(itemStack, msg.code, msg.fileName)` を呼び出して、コードとファイル名を `ItemStack` の `CompoundTag` に保存。
    8. 最後に `ctx.get().setPacketHandled(true)` を呼び出して処理完了を通知。

- **呼び出し元**: Forge ネットワークイベントハンドラ。
- **呼び出し先**:
    - `VSCodeBlockEntity#getItemInSlot()`
    - `VSCodeBlockScreen#setCode(ItemStack, String, String)`
- **エラーハンドリング**:
    - `player == null` または `blockEntity == null` の場合は即座に `return`。
    - 安全性を確保するためにメインスレッド内で処理される。

---

## 🔗 呼び出し関係図
```
クライアント
└─ sendToServer(new SaveCodePacket(fileName, code, position))
↓
サーバー
├─ SaveCodePacket.handle()
├─ VSCodeBlockEntity.getItemInSlot()
└─ VSCodeBlockScreen.setCode(itemStack, code, fileName)
```

---

## 🧩 外部依存関係

| クラス名 | パッケージ | 用途 |
|-----------|-------------|------|
| `FriendlyByteBuf` | `net.minecraft.network` | Forge のバイトデータ送受信用バッファ。UTF文字列や座標の読み書きを行う。 |
| `NetworkEvent.Context` | `net.minecraftforge.network` | 非同期通信イベントの制御を行う。`enqueueWork()` によりメインスレッドで安全に実行可能。 |
| `ServerPlayer` | `net.minecraft.server.level` | サーバー上のプレイヤー情報を取得し、ブロックエンティティ操作に使用。 |
| `BlockPos` | `net.minecraft.core` | ワールド内のブロック座標を表すクラス。 |
| `ItemStack` | `net.minecraft.world.item` | ブロック内のスロットに格納されたアイテム情報を保持。 |
| `VSCodeBlockEntity` | `com.example.examplemod.Blocks.VSCodeBlock` | カスタム VSCode ブロックのエンティティクラス。コード保存対象。 |
| `VSCodeBlockScreen` | `com.example.examplemod.Blocks.VSCodeBlock` | ブロック内の UI／表示部分を管理し、`setCode()` でコード保存処理を行う。 |

---

## 📦 ファイル別概要

| ファイル名 | 主な責務 | 主な関数 |
|-------------|-----------|-----------|
| `SaveCodePacket.java` | クライアントからサーバーへのコード保存要求をパケットで送信し、対象の VSCode ブロック内にデータを反映する。 | `encode()`, `decode()`, `handle()` |

---

## 🧭 処理全体の流れ（ストーリー形式）

1. プレイヤーが Minecraft 内の VSCode ブロックを開き、コードを入力または編集する。
2. 「保存」操作を行うと、クライアント側で `SaveCodePacket` が作成され、`fileName`・`code`・`position` がパケットに格納される。
3. パケットがサーバーに送信されると、`handle()` が実行される。
4. サーバーは該当するブロック (`VSCodeBlockEntity`) を位置情報から取得し、内部スロットのアイテム (`ItemStack`) を取り出す。
5. `VSCodeBlockScreen.setCode()` を呼び出して、コードとファイル名をアイテムの NBT (`CompoundTag`) に保存する。
6. これにより、Minecraft 内で「VSCode ブロックに保存されたコードデータ」が永続的に保持される。

---

## 🧩 補足：設計上のポイント
- **スレッドセーフティ**  
  Forge のネットワークパケットは別スレッドで処理されるため、`enqueueWork()` によってメインスレッドで安全にブロック操作を行う設計となっている。
- **拡張性**  
  将来的には `VSCodeBlockEntity` 内に複数ファイル保存やコンパイル機能を追加することで、Minecraft 内で完全なコード開発環境を実現できる。

---

このクラスは、**Minecraft 内のブロックとネットワーク通信を組み合わせたコード保存システム**を実現しており、仮想的なプログラミング環境を構築する基盤となる重要な要素である。
