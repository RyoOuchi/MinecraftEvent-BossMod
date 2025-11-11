# 📘 コードドキュメント

## 概要
`ServerBlock` クラスは、Minecraft Mod における「サーバブロック」の実装です。このブロックは実際のサーバハードウェアを模したもので、プレイヤーが設置・破壊・利用することができます。主に以下の責務を持ちます。

- サーバブロックが撤去された際にサーバ保存データから該当するサーバ記録を削除し、登録済みサーバ一覧をログに表示する。
- プレイヤーが右クリックでブロックを使用した際に、クライアント側でサーバ設定画面 (`ServerDomainScreen`) を開く。
- ブロックエンティティ (`ServerBlockEntity`) を生成し、サーバサイドでのティック処理を委任する。

本クラスはブロック本体であり、ネットワーク通信やファイルシステムの処理は `ServerBlockEntity` 側に委任しているため、処理内容はシンプルですが他モジュールとの連携が密接です。

---

## 🌐 グローバル変数一覧
このクラスではインスタンスフィールドや静的フィールドの宣言が無く、グローバル変数は存在しません。そのため、状態はすべて引数や外部クラス (`ServerSavedData` 等) から取得・更新されます。

---

## 🧩 関数一覧

### コンストラクタ `ServerBlock()`
- **概要**: サーバブロックを初期化し、素材を石材 (`Material.STONE`) に設定します。
- **引数**: なし。
- **処理内容**: 親クラス `Block` のコンストラクタに `Properties.of(Material.STONE)` を渡してブロック性質を設定します。
- **呼び出し元**: Minecraft のブロック登録処理。ワールドにブロックが置かれたときに呼び出されます。

### `onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) -> void`
- **概要**: ブロックが破壊・置き換えなどでワールドから除去された際に呼ばれ、サーバ登録情報を更新します。
- **パラメータ**:
    - `pState`: 旧ブロック状態。
    - `pLevel`: 実行中のワールド。クライアントかサーバかを判断します。
    - `pPos`: ブロックのワールド座標。
    - `pNewState`: 置き換え後のブロック状態。
    - `pIsMoving`: ピストンなどによる移動かどうか。
- **処理内容**:
    1. 親クラス `Block` の `onRemove()` を呼び出します。
    2. サーバサイド (`!pLevel.isClientSide()`) かつワールドが `ServerLevel` である場合、`ServerSavedData` を取得し、現在登録されているサーバ一覧 (`getServers()`) をログ出力します。
    3. `ServerSavedData.removeServer(pPos)` を呼び、除去された位置に対応するサーバ記録を削除し、結果をログに出力します。
- **呼び出し元**: ブロックがワールドから削除された際、Minecraft エンジンが自動的に呼び出します。
- **呼び出し先**: `ServerSavedData.get()`, `ServerSavedData.removeServer()`。

### `use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) -> InteractionResult`
- **概要**: プレイヤーがブロックを右クリックした際の挙動を定義します。
- **パラメータ**:
    - `pState`: ブロック状態。
    - `pLevel`: 現在のワールド。
    - `pPos`: ブロック位置。
    - `pPlayer`: プレイヤーエンティティ。
    - `pHand`: 使用した手 (main/off hand)。
    - `pHit`: 当たり判定の詳細。
- **処理内容**:
    1. クライアントサイドである場合 (`pLevel.isClientSide`) にのみ、`Minecraft.getInstance().setScreen(new ServerDomainScreen(pPos))` を呼び出し、サーバドメイン設定画面を開きます。
    2. 最後に `super.use(...)` を呼び、既定のブロック使用挙動を返します（例えば何もしない、常に成功を返すなど、親クラスに依存）。
- **戻り値**: `InteractionResult` – ブロックがインタラクトを処理したかどうかを表し、親クラスの実装に委任しています。
- **呼び出し元**: プレイヤーがブロックを右クリックしたとき、Minecraft エンジンが自動的に呼び出します。
- **呼び出し先**: クライアントの場合に `ServerDomainScreen` のコンストラクタ、`Minecraft.getInstance().setScreen()`、および親クラスの `use()` メソッド。

### `newBlockEntity(BlockPos pPos, BlockState pState) -> BlockEntity?`
- **概要**: サーバブロックに紐付けるブロックエンティティ（`ServerBlockEntity`）の生成処理を行います。
- **パラメータ**:
    - `pPos`: ブロックの座標。
    - `pState`: ブロック状態。
- **処理内容**: 新しい `ServerBlockEntity` インスタンスを作成し、位置と状態を指定して返します。`@Nullable` として宣言されていますが、この実装では常に非 null を返します。
- **戻り値**: `ServerBlockEntity` のインスタンス。
- **呼び出し元**: Minecraft のチャンク読み込みやブロック設置時に自動的に呼ばれます。

### `getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) -> BlockEntityTicker<T>?`
- **概要**: ブロックエンティティのティック処理を登録します。サーバサイドのみ処理を実行し、毎 tick `ServerBlockEntity.tickServer()` を呼び出します。
- **パラメータ**:
    - `pLevel`: 現在のワールド。
    - `pState`: ブロック状態。
    - `pBlockEntityType`: ブロックエンティティの型。
- **処理内容**:
    1. クライアントサイドの場合 (`pLevel.isClientSide`) はティッカーを返さず (`null`) にします。これによりクライアント側で余計な処理を行わない。
    2. サーバサイドの場合はラムダ式を返し、毎 tick `blockEntity.tickServer()` を呼び出してサーバブロックエンティティの処理を行います。`blockEntity` が `ServerBlockEntity` である場合にのみキャストして呼び出します。
- **戻り値**: サーバサイドでは `BlockEntityTicker<T>`、クライアントサイドでは `null`。
- **呼び出し元**: ワールドティックの際に Minecraft エンジンが自動的に呼び出します。
- **呼び出し先**: `ServerBlockEntity.tickServer()`。

### `getListener(Level pLevel, T pBlockEntity) -> GameEventListener?`
- **概要**: ゲームイベントリスナーを返します。ここでは `EntityBlock.super.getListener()` を利用し、特別なリスナーは定義していません。
- **パラメータ**:
    - `pLevel`: ワールド。
    - `pBlockEntity`: ブロックエンティティ。
- **戻り値**: 親インターフェイスから取得したリスナー。通常は `null` ですが、親のデフォルト実装に従います。
- **呼び出し元**: ゲームイベント（例：振動センサー）関連の処理が必要な場面で呼び出されます。

---

## 🔁 呼び出し関係図（関数依存）
サーバブロック内の主要なメソッド間の関係を図示します。
```
Player right‑click
└─ use()                          … クライアントなら ServerDomainScreen を表示

Block removal
└─ onRemove()
├─ ServerSavedData.get()
└─ ServerSavedData.removeServer()

Chunk/Block loaded
└─ newBlockEntity()               … 新しい ServerBlockEntity を返す

World tick (server side)
└─ getTicker() → BlockEntityTicker
└─ tickServer() on ServerBlockEntity
```
---

## ⚙️ 外部依存関係
| クラス／ライブラリ | 用途と理由 |
|------------------|-----------|
| **`ServerBlockEntity`** | サーバブロックに紐付けられるブロックエンティティ。ネットワーク通信やファイルシステム操作などサーバ固有のロジックを実装し、`tickServer()` で処理を行う。 |
| **`ServerSavedData`** | サーバの登録情報を保持するデータクラス。ブロック削除時に `removeServer()` を呼び、サーバ一覧の管理を行う。 |
| **`DnsServerSavedData`** | DNS サーバ登録データを保持するクラス。本ファイルではインポートされているが直接は利用されない（可能性として将来の拡張用）。 |
| **`ServerDomainScreen`** | クライアントサイドでサーバのドメイン設定を行う GUI。`use()` で表示される。 |
| **`Minecraft`** | クライアントサイドのゲームインスタンスにアクセスするために使用。GUI 画面表示に必要。 |
| **`InteractionResult`, `Player`, `InteractionHand`, `BlockHitResult`** | プレイヤーのインタラクション結果やハンド情報、ヒット判定を表すクラス。`use()` の引数として使用される。 |
| **`BlockEntityType`**, **`GameEventListener`** | ブロックエンティティやゲームイベントリスナーの型情報。ティック処理とイベント処理の登録に使用される。 |
| **`Material`**, **`Properties`** | ブロックの素材や特性を指定する Minecraft API。コンストラクタでブロックの基礎性質を設定する。 |
| **`Block`, `EntityBlock`** | ブロックの基本クラスおよびブロックエンティティを持つブロックのインターフェース。`ServerBlock` はこれらを継承/実装している。 |

---

## 📄 ファイル別概要
| ファイル／クラス名 | 主な責務 | 主な関数 |
|-----------------|-----------|------------|
| **`ServerBlock.java`** | サーバブロック本体の実装。プレイヤー操作やブロック破壊に応じた処理を行い、ブロックエンティティを生成・管理する。 | コンストラクタ、`onRemove()`, `use()`, `newBlockEntity()`, `getTicker()`, `getListener()` |
| **`ServerBlockEntity.java`** | サーバブロックのロジック本体。TCP ハンドシェイクやファイルシステムとのやりとり、スライディングウィンドウによるデータ送受信など複雑な処理を担当する。 | `tickServer()`, `receiveDataPacket()`, `handleHandshake()`, `handleDataTransfer()` 等 |
| **`ServerDomainScreen.java`** | クライアント側の GUI 画面。サーバブロックに紐づくドメイン名や設定を表示・入力する。 | 画面描画・入力イベント |
| **`ServerSavedData.java`** | ワールドに登録されたサーバ情報（名前と位置）を保存し、NBT を通して永続化する。 | `getServers()`, `addServer()`, `removeServer()` |

---

## 💬 総評
`ServerBlock` は、サーバブロックのインタラクションとライフサイクルを司るシンプルなクラスであり、ネットワークやファイルシステムの詳細処理は `ServerBlockEntity` に委任しています。設置・破壊の際にはサーバデータを適切に更新し、プレイヤーがブロックを右クリックしたときは GUI を開くといった基本的な UI 連携を実装します。ブロック自体には状態を保持する変数が無いため、副作用が少なく保守しやすい設計です。デバッグ時にはログ出力によりサーバリストの確認や削除結果を把握でき、開発者にとって分かりやすい実装となっています。