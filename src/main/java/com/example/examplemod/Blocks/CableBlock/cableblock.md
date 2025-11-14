# 📘 コードドキュメント

## 概要
`CableBlock` クラスは、Minecraft Mod 内におけるケーブルブロックの具体的な振る舞いを定義します。このケーブルブロックは隣接するケーブルとの接続状態を6方向（北・南・東・西・上・下）で持ち、ネットワーク通信の物理層として機能します。主な処理は以下の通りです。

- ブロックの配置や隣接ブロックの変化に応じて、各方向の接続状態 (`BooleanProperty`) を自動的に更新する。
- プレースメント時にはプレイヤーの視線方向を基に初期の接続方向をセットする。
- サーバ側でケーブルブロックエンティティ (`CableBlockEntity`) の `tickServer()` を呼び出し、ネットワークパケットの中継処理を行う。
- ブロックエンティティ生成やゲームイベントリスナーは親クラスに委任する。

このクラス自体は単に接続状態を管理するだけで、パケット処理などのロジックは `CableBlockEntity` に委ねています。

---

## 🌐 グローバル変数一覧
`CableBlock` は静的なブロックステートプロパティを6方向分定義します。これらのプロパティは各ケーブルブロックの接続状態を表すブール値として使用されます。

| 変数名 | 型 | 初期値 | 使用箇所 | 説明 |
|--------|----|--------|----------|------|
| `NORTH` | `BooleanProperty` | `BlockStateProperties.NORTH` | `createBlockStateDefinition()`, `getPropertyFor()`, 各種更新処理 | ブロックの北側とケーブルが接続しているかどうかを示す。 |
| `SOUTH` | `BooleanProperty` | `BlockStateProperties.SOUTH` | 同上 | 南側の接続状態。 |
| `EAST` | `BooleanProperty` | `BlockStateProperties.EAST` | 同上 | 東側の接続状態。 |
| `WEST` | `BooleanProperty` | `BlockStateProperties.WEST` | 同上 | 西側の接続状態。 |
| `UP` | `BooleanProperty` | `BlockStateProperties.UP` | 同上 | 上側の接続状態。 |
| `DOWN` | `BooleanProperty` | `BlockStateProperties.DOWN` | 同上 | 下側の接続状態。 |

各プロパティは `createBlockStateDefinition()` でステート定義に登録され、デフォルトではコンストラクタで `false` に初期化されます。

---

## 🧩 関数一覧

### コンストラクタ `CableBlock()`
- **概要**: ケーブルブロックを初期化し、オクルージョンなしで光源レベル7の石材として設定します。また、全方向の接続状態を `false` にしたデフォルトステートを登録します。
- **処理内容**:
    1. `super(Properties.of(Material.STONE).noOcclusion().lightLevel(light -> 7))` を呼び、ブロックの性質を設定。
    2. `registerDefaultState()` を呼び出し、`NORTH`, `SOUTH`, `EAST`, `WEST`, `UP`, `DOWN` 全てを `false` に設定したブロックステートを登録。

### `protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder)`
- **概要**: ブロックステートに6方向の `BooleanProperty` を登録します。
- **処理内容**: `pBuilder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN)` を呼び出し、接続フラグをステート定義に追加します。
- **呼び出し元**: Minecraft のブロックステート登録処理中に自動的に呼び出される。

### `BlockState updateShape(BlockState pState, Direction pDirection, BlockState pNeighborState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pNeighborPos)`
- **概要**: 隣接ブロックが更新された際に、当該方向への接続フラグを更新します。
- **処理内容**:
    1. `canConnectTo(pNeighborState)` を呼び出して隣接ブロックがケーブルかどうか判定。
    2. `getPropertyFor(pDirection)` で対象方向に対応する `BooleanProperty` を取得。
    3. `pState.setValue(property, connected)` により接続状態を更新して新しい `BlockState` を返す。
- **戻り値**: 更新されたステート。
- **呼び出し元**: ブロック配置、隣接ブロック変更などのイベントで Minecraft エンジンから呼び出される。

### `private boolean canConnectTo(BlockState neighborState)`
- **概要**: 隣接ブロックがケーブルブロックかどうか判定します。
- **戻り値**: `neighborState.getBlock() instanceof CableBlock` の評価結果。ケーブルブロックなら `true`。
- **呼び出し元**: `updateShape()`, `updateConnections()`。

### `void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving)`
- **概要**: ブロックがワールドに置かれたときに呼ばれ、周囲の接続状態を初期設定します。
- **処理内容**: `updateConnections(pLevel, pPos, pState)` を呼び、全方向の接続フラグを計算して更新します。
- **呼び出し元**: ブロックの配置時。

### `void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean moving)`
- **概要**: 隣接ブロックが変化した際に呼ばれ、ケーブルの接続状態を再計算します。
- **処理内容**:
    1. 親クラス `Block.neighborChanged()` を呼び出す。
    2. `updateConnections(level, pos, state)` により全方向の接続フラグを更新。
- **呼び出し元**: Minecraft エンジンが隣接ブロックの更新を検知したとき。

### `private void updateConnections(LevelAccessor level, BlockPos pos, BlockState state)`
- **概要**: 指定位置のケーブルブロックの全方向について、隣接ブロックがケーブルかどうかを検査し、ステートを更新します。
- **処理内容**:
    1. `for (Direction dir : Direction.values())` で全方向をループ。
    2. `neighborPos = pos.relative(dir)` で隣接位置を取得し、`neighborState = level.getBlockState(neighborPos)` でブロック状態を取得。
    3. `connected = canConnectTo(neighborState)` で接続判定し、`state = state.setValue(getPropertyFor(dir), connected)` で状態を更新。
    4. ループ終了後、`level.setBlock(pos, state, 3)` でワールドのブロックステートを更新。
- **呼び出し元**: `onPlace()`, `neighborChanged()`。

### `private BooleanProperty getPropertyFor(Direction dir)`
- **概要**: 方向に対応する `BooleanProperty` を返します。
- **処理内容**: `switch` 文で `dir` に応じたプロパティ (`NORTH`, `SOUTH`, `EAST`, `WEST`, `UP`, `DOWN`) を返す。
- **呼び出し元**: `updateShape()`, `updateConnections()`, `getStateForPlacement()`。

### `BlockState getStateForPlacement(BlockPlaceContext context)`
- **概要**: ブロックを配置する際に初期状態を決定します。プレイヤーの視線方向に対して反対向きに接続フラグを立てます。
- **処理内容**:
    1. `Direction dir = context.getNearestLookingDirection().getOpposite()` でプレイヤーが向いている方向の反対側を取得。
    2. `state = this.defaultBlockState()` からデフォルトステートを取得。
    3. 全方向について、`state = state.setValue(getPropertyFor(d), d == dir)` とし、プレイヤーから見て奥側の方向のみ `true` に設定。
- **戻り値**: 初期化されたブロックステート。
- **呼び出し元**: ブロック配置時にエンジンから呼び出される。

### `@Nullable BlockEntity newBlockEntity(BlockPos pPos, BlockState pState)`
- **概要**: ケーブルブロックに紐付ける `CableBlockEntity` を生成します。
- **戻り値**: 新しい `CableBlockEntity`。常に非 null。
- **呼び出し元**: チャンク読み込み・ブロック設置時に自動的に呼ばれる。

### `@Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType)`
- **概要**: ブロックエンティティのティック処理を登録します。
- **処理内容**:
    1. クライアントサイドであれば `null` を返し、ティック処理を行わない。
    2. サーバサイドであればラムダを返し、`te instanceof CableBlockEntity` を検査のうえ `blockEntity.tickServer(level, pos)` を呼び出す。
- **戻り値**: サーバサイドでは `BlockEntityTicker<T>`、クライアントサイドでは `null`。
- **呼び出し元**: ワールドのティック処理により自動的に呼ばれる。

### `@Nullable <T extends BlockEntity> GameEventListener getListener(Level pLevel, T pBlockEntity)`
- **概要**: このブロックに関連するゲームイベントリスナーを返します。ここでは特別なリスナーを定義せず、親クラスの実装に委任します。
- **戻り値**: デフォルトのゲームイベントリスナー。
- **呼び出し元**: 振動センサーなどのイベント処理が行われる際。

---

## 🔁 呼び出し関係図（関数依存）
```
Block placed
├─ onPlace()
│    └─ updateConnections()

Neighbor block changed
├─ neighborChanged()
│    ├─ Block.neighborChanged() (super)
│    └─ updateConnections()

updateShape()
├─ canConnectTo()
└─ getPropertyFor()

updateConnections()
├─ Direction.values()
├─ canConnectTo()
├─ getPropertyFor()
└─ level.setBlock()

getStateForPlacement()
├─ context.getNearestLookingDirection()
├─ getPropertyFor()
└─ defaultBlockState()

BlockEntity creation
└─ newBlockEntity() → CableBlockEntity(pPos, pState)

World tick (server side)
└─ getTicker() → BlockEntityTicker
└─ tickServer() on CableBlockEntity

Game events
└─ getListener()  (親クラスを利用)
```
---

## ⚙️ 外部依存関係
| クラス／ライブラリ | 用途 |
|------------------|-----|
| **`BlockStateProperties`** | `NORTH`, `SOUTH` 等の方向性 `BooleanProperty` を取得するために使用。 |
| **`Material`** | ブロックの材質を設定するため (`STONE`)。 |
| **`Level`, `LevelAccessor`** | ブロックの状態や周囲のブロックを取得し、更新する際に使用。 |
| **`Direction`** | 上下左右前後の6方向を表す列挙型。接続判定や初期ステート設定に使用。 |
| **`BlockPlaceContext`** | ブロックを配置する際のコンテキスト情報。プレイヤーの視線方向を取得するために使用。 |
| **`BlockEntity`**, **`BlockEntityTicker`**, **`CableBlockEntity`** | ケーブルブロックのエンティティの生成・毎ティック処理を管理するために使用。 |
| **`GameEventListener`** | ゲームイベント（振動など）のリスナー。ここでは親クラスの実装を利用。 |

---

## 📄 ファイル別概要
| ファイル／クラス名 | 主な責務 | 主なメソッド |
|--------------------|-----------|-------------|
| **`CableBlock.java`** | ケーブルブロックの外観と接続状態を管理するブロック本体。 | コンストラクタ、`createBlockStateDefinition()`, `updateShape()`, `onPlace()`, `neighborChanged()`, `updateConnections()`, `getStateForPlacement()`, `newBlockEntity()`, `getTicker()` |
| **`CableBlockEntity.java`** | ケーブルブロックの挙動やデータパケット中継を行うブロックエンティティ。 | `enqueuePacket()`, `tickServer()`, `processNextPacket()` など |

---

## 💬 総評
`CableBlock` はネットワークケーブルの物理的な接続状態を管理するために精巧に設計されています。6方向それぞれに `BooleanProperty` を用いて接続情報を保持し、ブロックの配置や隣接ブロックの変化に応じて自動的に接続フラグを更新します。これにより、ケーブルの自動接続や切断が直感的に行われ、ネットワークトポロジの構築が容易になります。

デフォルトステートの初期化やプレースメント時の視線方向に基づく接続設定など、ユーザビリティを意識した工夫も見られます。データ転送に関するロジックは `CableBlockEntity` に任せられているため、ブロック本体の責務は明確であり、単一責務原則に沿った実装となっています。全体として、高い保守性と拡張性を備えたブロック設計と言えるでしょう。