# 📘 コードドキュメント

## 概要
`DNSServerBlock` クラスは、Minecraft Mod における DNS サーバブロック本体の実装です。このブロックは DNS サーバ機能を提供する `DNSServerBlockEntity` と連携し、ブロックがワールドに配置・破壊された際に DNS サーバ登録情報を更新します。また、サーバサイドの毎ティック処理を `DNSServerBlockEntity` に委任します。主な責務は以下の通りです。

- ブロックエンティティ (`DNSServerBlockEntity`) の生成と管理。
- ブロック配置時に DNS サーバ登録データ (`DnsServerSavedData`) に新しいサーバ位置を追加。
- ブロック破壊時に DNS サーバ登録データから該当位置を削除。
- サーバサイドで DNS サーバブロックエンティティの `tickServer()` を定期的に呼び出す。

このクラスは見た目や物理特性を持たないシンプルな石ブロックとして設定され、ブロック自体は状態を持たずにデータストレージの役割を果たします。

---

## 🌐 グローバル変数一覧
このブロッククラスにはインスタンスフィールドや静的フィールドはなく、状態は全てブロックエンティティや保存データ側で管理されます。

---

## 🧩 関数一覧

### コンストラクタ `DNSServerBlock()`
- **概要**: DNS サーバブロックを初期化し、素材を石材 (`Material.STONE`) に設定します。
- **処理内容**: 親クラス `Block` のコンストラクタに `Properties.of(Material.STONE)` を渡してブロックの基本特性をセットします。
- **呼び出し元**: Mod がブロックを登録する際に自動的に呼び出されます。

### `@Nullable BlockEntity newBlockEntity(BlockPos pPos, BlockState pState)`
- **概要**: このブロックに紐付けられる `DNSServerBlockEntity` を生成します。
- **引数**:
    - `pPos` – ブロックエンティティの位置。
    - `pState` – ブロック状態。
- **戻り値**: 新しい `DNSServerBlockEntity` インスタンス。
- **呼び出し元**: チャンク読み込みやブロック設置時に Minecraft エンジンが自動的に呼び出します。

### `@Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType)`
- **概要**: DNS サーバブロックエンティティのティック処理を定義します。
- **処理内容**:
    1. クライアントサイド (`pLevel.isClientSide`) であれば `null` を返し、ティック処理を行わない。
    2. サーバサイドであればラムダ式を返し、毎 tick `te instanceof DNSServerBlockEntity` を確認して `blockEntity.tickServer()` を呼び出す。
- **戻り値**: サーバサイドでは `BlockEntityTicker<T>`, クライアントサイドでは `null`。
- **呼び出し元**: ワールドのティック処理により自動的に呼び出されます。

### `@Nullable <T extends BlockEntity> GameEventListener getListener(Level pLevel, T pBlockEntity)`
- **概要**: ゲームイベントリスナーを返します。ここでは親クラスのデフォルト実装を使用します。
- **戻り値**: デフォルトの `GameEventListener` または `null`。
- **呼び出し元**: ゲームイベント（振動検知など）が必要なときに呼び出されますが、このブロックでは特別なリスナーを提供しません。

### `void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving)`
- **概要**: ブロックがワールドに配置されたときに呼び出され、DNS サーバ登録データにこのブロックの位置を追加します。
- **処理内容**:
    1. 親クラス `Block.onPlace()` を呼び出す。
    2. サーバサイド (`!pLevel.isClientSide`) かつ `pLevel` が `ServerLevel` であることを確認。
    3. `DnsServerSavedData.get(serverLevel)` で DNS サーバ登録データを取得し、`data.addDnsServer(pPos)` を呼び出して位置を保存。
    4. コンソールに追加ログを出力。
- **呼び出し元**: ブロック設置時にエンジンから自動的に呼び出されます。

### `void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving)`
- **概要**: ブロックが破壊または置き換えられた際に呼び出され、登録データからこのサーバ位置を削除します。
- **処理内容**:
    1. 親クラス `Block.onRemove()` を呼び出す。
    2. サーバサイドかつ `pLevel` が `ServerLevel` であることを確認。
    3. `DnsServerSavedData.get(serverLevel)` から DNS サーバ登録データを取得し、`data.removeDnsServer(pPos)` を呼び出して位置を削除。
    4. コンソールに削除ログを出力。
- **呼び出し元**: ブロック破壊や置き換え時にエンジンから呼び出されます。

---

## 🔁 呼び出し関係図（関数依存）
```
Block placed
├─ onPlace()
│    ├─ Block.onPlace() (super)
│    └─ DnsServerSavedData.addDnsServer(pPos)

Block removed
├─ onRemove()
│    ├─ Block.onRemove() (super)
│    └─ DnsServerSavedData.removeDnsServer(pPos)

World tick (server side)
└─ getTicker() → BlockEntityTicker
└─ tickServer() on DNSServerBlockEntity

Game events
└─ getListener()  (親クラスを利用)

BlockEntity creation
└─ newBlockEntity() → DNSServerBlockEntity(pPos, pState)
```
---

## ⚙️ 外部依存関係
| クラス／ライブラリ | 用途 |
|------------------|-----|
| **`DNSServerBlockEntity`** | DNS サーバ機能を実装したブロックエンティティ。`tickServer()` により DNS クエリ応答などの処理を行う。 |
| **`DnsServerSavedData`** | ワールドに存在する DNS サーバブロックの位置を保存するデータクラス。`addDnsServer()` や `removeDnsServer()` で位置の登録・削除を行う。 |
| **`ServerLevel`** | サーバワールドのインスタンス。データ保存やブロックエンティティ管理のために使用。 |
| **`BlockEntity`**, **`BlockEntityTicker`**, **`GameEventListener`** | ブロックエンティティの生成・毎ティック処理・イベントリスナー管理を行う。 |
| **`Material`** | ブロックの材質を設定。ここでは石材 (`STONE`) が使用される。 |
| **`BlockPos`**, **`BlockState`**, **`Level`** | ブロックの位置や状態、ワールド情報の取得・変更に使用。 |

---

## 📄 ファイル別概要
| ファイル名／クラス名 | 主な責務 | 主なメソッド |
|--------------------|-----------|--------------|
| **`DNSServerBlock.java`** | DNS サーバブロックの本体。ブロックエンティティ生成、配置・破壊時の登録更新、ティック処理登録を行う。 | コンストラクタ、`newBlockEntity()`, `getTicker()`, `getListener()`, `onPlace()`, `onRemove()` |
| **`DNSServerBlockEntity.java`** | DNS サーバの具体的な処理を行うブロックエンティティ。DNS クエリ受信・応答生成などを担当。 | `tickServer()`, `receiveDataPacket()` 等 |
| **`DnsServerSavedData.java`** | サーバワールドに存在する DNS サーバブロックの位置を永続的に保存するデータクラス。 | `addDnsServer()`, `removeDnsServer()`, `getDnsServers()` |

---

## 💬 総評
`DNSServerBlock` クラスは、DNS サーバブロックのライフサイクル管理をシンプルにまとめた実装です。ブロックの配置・撤去時に `DnsServerSavedData` を更新することでワールド内の DNS サーバ一覧を常に正確に保持し、`DNSServerBlockEntity` に処理を委任することで責務分離を実現しています。クライアント側では特に処理を行わず、サーバサイドでのみティック処理が動作するため、余計な負荷をかけない設計となっています。全体的に明確な責務分担と単純なロジックから構成されており、他のネットワーク関連ブロックとの連携も容易です。