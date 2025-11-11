# コードドキュメント

## 概要
このクラス `AddServerPacket` は、Minecraft Forge のネットワーク通信システムを利用して、クライアント側から「新しいサーバーを追加する」要求をサーバー側に送信するためのパケット定義である。  
主な目的は、クライアントから受け取ったサーバーのドメイン名と座標 (`BlockPos`) をサーバーワールドの永続データ (`ServerSavedData`) に登録することである。

処理の流れは以下のようになる：
1. クライアント側で `AddServerPacket` が生成される。
2. パケットが `encode()` によってバイトバッファ (`FriendlyByteBuf`) にシリアライズされ、送信される。
3. サーバー側で `decode()` により復元される。
4. `handle()` が呼ばれ、指定された座標にサーバー情報が登録される。

---

## グローバル変数一覧

| 変数名 | 型 | 初期値 | 使用箇所 | 説明 |
|--------|----|--------|----------|------|
| `domain` | `String` | コンストラクタ引数で指定 | `encode()`, `handle()` | 追加するサーバーのドメイン名（ユニーク識別用） |
| `pos` | `BlockPos` | コンストラクタ引数で指定 | `encode()`, `handle()` | サーバーブロックの座標位置（ワールド上の位置） |

---

## 関数一覧

### `public AddServerPacket(String domain, BlockPos pos)`
- **概要**: パケットの初期化コンストラクタ。
- **引数**:
    - `domain`: サーバー識別用のドメイン名。
    - `pos`: サーバーブロックのワールド座標。
- **処理**: インスタンス変数 `domain` と `pos` を初期化する。
- **呼び出し元**: クライアント側で新しいサーバーを登録する際に呼び出される。

---

### `public static void encode(AddServerPacket msg, FriendlyByteBuf buf)`
- **概要**: パケットをシリアライズ（エンコード）し、バイトバッファへ書き込む。
- **引数**:
    - `msg`: 送信するパケットインスタンス。
    - `buf`: Forgeの `FriendlyByteBuf` バッファ。
- **主な処理**:
    1. `domain` を文字列として書き込む (`writeUtf()`)。
    2. `pos` を座標データとして書き込む (`writeBlockPos()`)。
- **呼び出し元**: Forge のネットワークレイヤーが送信時に自動で呼び出す。

---

### `public static AddServerPacket decode(FriendlyByteBuf buf)`
- **概要**: エンコードされたパケットデータを復元（デコード）する。
- **引数**:
    - `buf`: クライアントから送信されたバイトデータ。
- **戻り値**: 復元された `AddServerPacket` オブジェクト。
- **主な処理**:
    1. `readUtf()` でドメイン名を読み取る。
    2. `readBlockPos()` で座標データを復元。
    3. それらを利用して新しいパケットを生成して返す。
- **呼び出し元**: サーバー受信側のネットワークイベント。

---

### `public void handle(Supplier<NetworkEvent.Context> context)`
- **概要**: サーバー側で受信したパケットを処理するメインメソッド。
- **引数**:
    - `context`: Forgeのネットワークイベントコンテキスト。
- **主な処理の流れ**:
    1. `context.get().enqueueWork()` を利用してサーバースレッド上で安全に処理を実行。
    2. 送信元プレイヤー (`ServerPlayer`) を取得。
    3. そのプレイヤーの属する `ServerLevel`（ワールドインスタンス）を参照。
    4. `ServerSavedData.get(serverLevel)` によりサーバーデータを取得。
    5. `addServer(domain, pos)` を呼び出してサーバー情報を登録。
    6. コンソールにログ (`System.out.println`) を出力。
    7. `context.get().setPacketHandled(true)` でパケットを処理済みとしてマーク。

- **呼び出し元**: Forge のサーバーパケット受信イベント。
- **呼び出し先**:
    - `ServerSavedData.get(ServerLevel)`
    - `ServerSavedData.addServer(String, BlockPos)`

- **エラーハンドリング**:
    - 送信者 (`player`) が `null` の場合は早期 return（安全対策）。
    - それ以外の例外処理は Forge ネットワークに委任される。

---

## 呼び出し関係図（関数依存）
```
クライアント側
↓
AddServerPacket(domain, pos)
↓
encode() ──→ FriendlyByteBuf
↓（ネットワーク送信）
サーバー側
↓
decode()
↓
handle(context)
└─→ ServerSavedData.get(serverLevel)
└─→ addServer(domain, pos)
```
---

## 外部依存関係
| クラス / パッケージ | 目的 | 使用箇所 |
|----------------------|------|-----------|
| `net.minecraftforge.network.NetworkEvent` | パケット処理イベントを管理 | `handle()` |
| `net.minecraft.server.level.ServerPlayer` | 送信プレイヤー情報の取得 | `handle()` |
| `net.minecraft.server.level.ServerLevel` | サーバーワールドへのアクセス | `handle()` |
| `com.example.examplemod.ServerData.ServerSavedData` | サーバーデータ永続化（NBT） | `handle()` |
| `net.minecraft.network.FriendlyByteBuf` | パケットのシリアライズ／デシリアライズ | `encode()`, `decode()` |

---

## ファイル別概要

| ファイル名 | 主な責務 | 主な関数 |
|-------------|-----------|-----------|
| `AddServerPacket.java` | クライアントからサーバー追加命令を送信するネットワークパケットの定義 | `encode()`, `decode()`, `handle()` |
| `ServerSavedData.java` | 永続化されたサーバーデータの登録／削除を管理 | `get()`, `addServer()`, `removeServer()` |

---

## 技術的補足
- 本クラスは Forge のネットワーク層 (`SimpleChannel`) に登録され、クライアント→サーバー通信の一部として動作する。
- `enqueueWork()` により、スレッドセーフなサーバーロジック実行が保証されている。
- 永続化は `ServerSavedData` 経由で自動的に NBT ファイルへ反映されるため、ゲーム終了後もサーバー情報が維持される。

---

## 総括
`AddServerPacket` は、ネットワーク経由でサーバー情報を安全に登録するための通信レイヤーを実現しており、  
Minecraft 内で構築される仮想ネットワークの「DNS登録」や「サーバー構成更新」を支える基盤的なクラスである。  
この仕組みにより、クライアントが追加したサーバー情報がサーバーワールド全体で共有・永続化され、  
ネットワーク通信システム全体の一貫性が保たれている。