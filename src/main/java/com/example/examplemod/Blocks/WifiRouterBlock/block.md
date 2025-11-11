# 📘 コードドキュメント

## 概要
`WifiRouterBlock` クラスは、Minecraft Mod における Wi‑Fi ルータブロックの本体を実装しています。このブロックはネットワーク中継を担当する `WifiRouterBlockEntity` と連携し、周囲のケーブルやルータと接続してルーティング処理を行います。主な責務は以下の通りです。

- ルータブロックエンティティ (`WifiRouterBlockEntity`) の生成と管理。
- プレイヤーがブロックを右クリックしたときに、ルータネットワークの状態を読み込み可視化する。
- ブロックの設置時に、上部に DNS サーバブロックが存在する場合は接続を確認し、ログを出力する。
- サーバ側で `WifiRouterBlockEntity.tickServer()` を毎 tick 呼び出す。
- ブロック自体は状態を持たず、外部のエンティティとデータ構造に処理を委任する。

---

## 🌐 グローバル変数一覧
このクラスには状態を保持するインスタンスフィールドや静的フィールドは存在しません。ルータマップなどの情報はすべてブロックエンティティ側 (`WifiRouterBlockEntity`) に保存されます。

---

## 🧩 関数一覧

### コンストラクタ `WifiRouterBlock()`
- **概要**: ルータブロックを初期化します。石材 (`Material.STONE`) としてブロックの基本プロパティを設定します。
- **呼び出し元**: Mod のブロック登録時に自動的に呼び出されます。

### `@Nullable BlockEntity newBlockEntity(BlockPos pPos, BlockState pState)`
- **概要**: このブロックに紐づく `WifiRouterBlockEntity` を生成します。
- **戻り値**: 新しい `WifiRouterBlockEntity` インスタンス。
- **呼び出し元**: チャンク読み込みまたはブロック設置時にエンジンによって呼び出されます。

### `@Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType)`
- **概要**: ルータブロックエンティティの毎ティック処理を登録します。
- **処理内容**:
    1. クライアントサイド (`pLevel.isClientSide`) であれば `null` を返し、ティック処理を行わない。
    2. サーバサイドの場合はラムダ式を返し、`te instanceof WifiRouterBlockEntity` をチェックして `blockEntity.tickServer(level)` を呼び出します。
- **呼び出し元**: ワールドのサーバ側 tick 処理。

### `@Nullable <T extends BlockEntity> GameEventListener getListener(Level pLevel, T pBlockEntity)`
- **概要**: ブロックに関連するゲームイベントリスナーを返します。ここでは親クラスのデフォルト実装を使用します。
- **戻り値**: デフォルトの `GameEventListener` または `null`。
- **呼び出し元**: ゲームイベント（振動センサーなど）が関連する場面で呼び出されますが、このブロックでは特別なリスナーを定義しません。

### `InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit)`
- **概要**: プレイヤーがルータブロックを右クリックした際の挙動を定義します。
- **処理内容**:
    1. `WifiRouterBlockEntity be = (WifiRouterBlockEntity) pLevel.getBlockEntity(pPos)` でエンティティを取得。`null` なら失敗を返す。
    2. エンティティが存在すれば `be.loadDNSServers()` を呼び出し、DNS サーバ情報とネットワークを再読み込み。
    3. コンソールに「📶 WifiRouterBlock used! Current Router Map:」と出力し、`Graph routerMap = be.getRouterMap()` を取得。
    4. `routerMap.visualizeNetwork(pLevel, Blocks.AMETHYST_BLOCK)` を呼び、アメジストブロックでネットワークをデバッグ可視化。
    5. 最終的に `super.use()` を呼び、標準のブロック使用結果を返す。
- **戻り値**: 親クラスの `use()` の戻り値 (`InteractionResult`).
- **呼び出し元**: プレイヤーが右クリックしたとき。

### `void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving)`
- **概要**: ルータブロックがワールドに設置された際に呼ばれます。DNS サーバとの接続を確認します。
- **処理内容**:
    1. クライアントサイドであれば何もしない。
    2. サーバサイドの場合、`WifiRouterBlockEntity be = (WifiRouterBlockEntity) pLevel.getBlockEntity(pPos)` を取得。null ならエラーログを出して終了。
    3. `pLevel.getBlockEntity(pPos.above()) instanceof DNSServerBlockEntity` をチェックし、上のブロックが DNS サーバであれば「🔗 Connected WifiRouter at [pos] to DNS Server above it.」とログ出力。
    4. 最後に `super.onPlace()` を呼び、親クラスの処理を実行。
- **呼び出し元**: ブロックが配置されたとき。

---

## 🔁 呼び出し関係図（関数依存）
```
Block placed
└─ onPlace()
├─ if server side:
│    ├─ getBlockEntity()  → WifiRouterBlockEntity
│    ├─ check block above for DNSServerBlockEntity
│    └─ log connection
└─ Block.onPlace() (super)

Player right-click
└─ use()
├─ getBlockEntity()  → WifiRouterBlockEntity
├─ loadDNSServers() on WifiRouterBlockEntity
├─ getRouterMap() on WifiRouterBlockEntity
├─ visualizeNetwork() on Graph
└─ Block.use() (super)

Block entity creation
└─ newBlockEntity() → WifiRouterBlockEntity(pPos, pState)

World tick (server side)
└─ getTicker() → BlockEntityTicker
└─ tickServer() on WifiRouterBlockEntity

Game events
└─ getListener()  (親クラスを利用)
```
---

## ⚙️ 外部依存関係
| クラス／ライブラリ | 用途 |
|------------------|------|
| **`WifiRouterBlockEntity`** | 実際のルーティング処理とネットワーク状態を管理するブロックエンティティ。`loadDNSServers()`, `tickServer()`, `getRouterMap()` などを提供する。 |
| **`CableBlockEntity`**, **`DNSServerBlockEntity`** | インポートされていますが、このクラス内では直接使用されません。`WifiRouterBlockEntity` 内での処理に用いられます。 |
| **`Graph`** | ルータネットワークのトポロジーを表すデータ構造。`visualizeNetwork()` を使ってデバッグ表示に利用されます。 |
| **`Blocks.AMETHYST_BLOCK`** | ネットワークの可視化で使用するマーカー用ブロック。 |
| **`Material`** | ブロックの材質を設定。ここでは石材 (`STONE`)。 |
| **Minecraft API (`BlockPos`, `BlockState`, `Level`, `BlockEntity`, `BlockEntityTicker`, `GameEventListener`)** | ブロック位置や状態、ワールド情報、ブロックエンティティ管理に用いられます。 |

---

## 📄 ファイル別概要
| ファイル名／クラス名 | 主な責務 | 主なメソッド |
|--------------------|-----------|--------------|
| **`WifiRouterBlock.java`** | Wi‑Fi ルータブロックの本体実装。ブロックエンティティ生成、設置時の接続確認、ユーザーインタラクト時のネットワーク可視化などを担当。 | コンストラクタ、`newBlockEntity()`, `getTicker()`, `getListener()`, `use()`, `onPlace()` |
| **`WifiRouterBlockEntity.java`** | ルータのロジックとネットワーク経路探索、パケットルーティングを実装。DNS サーバとの通信やサーバリクエスト処理などを行う。 | `tickServer()`, `loadDNSServers()`, `getRouterMap()`, `performDNSRequest()` 等 |
| **`CableBlockEntity.java`** | ネットワークケーブルのパケット中継処理を行うブロックエンティティ。 | `enqueuePacket()`, `tickServer()`, `processNextPacket()` 等 |

---

## 💬 総評
`WifiRouterBlock` クラスは、ネットワークルータブロックの外枠を担い、ユーザーインタラクションや設置／破壊イベントに応じた処理を提供しています。ブロック自体は状態を保持せず、実際のネットワークロジックは `WifiRouterBlockEntity` に委任しているため責務が明確です。右クリック操作で DNS サーバ情報を読み込み、ネットワークをアメジストブロックで可視化できるなど、デバッグや学習用途にも配慮した設計となっています。設置時に上部の DNS サーバブロックとの接続を自動検出してログ出力する点も、ユーザーがネットワーク状態を把握する助けになります。全体として、他のネットワーク関連ブロックと連携しやすく、拡張性の高い実装です。