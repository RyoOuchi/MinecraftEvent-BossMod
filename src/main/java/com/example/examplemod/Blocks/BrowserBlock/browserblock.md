# 📘 コードドキュメント

## 概要
この `BrowserBlock` クラスは、ブラウザ機能を提供するブロックの本体を実装しています。プレイヤーがブロックを右クリックするとブラウザ検索画面を開き、サーバ側では `BrowserBlockEntity` に委任してネットワーク通信を処理します。主な責務は以下の通りです。

- ブロックが設置された位置に対応する `BrowserBlockEntity` を生成・管理する。
- クライアント側でブロックを右クリックした際に、検索画面 (`BrowserSearchScreen`) を表示する。
- サーバ側の毎ティック処理において、`BrowserBlockEntity` の `tickServer()` メソッドを呼び出す。
- ブロックのイベントリスナーは特に独自のものを提供せず、親クラスの実装を利用する。

このクラス自身は状態を保持しないため、ブロックに紐付く全てのロジックは `BrowserBlockEntity` に委ねられています。

---

## 🌐 グローバル変数一覧
このクラスにはインスタンスフィールドや静的フィールドは存在しません。状態管理はすべて `BrowserBlockEntity` に委譲されます。

---

## 🧩 関数一覧

### コンストラクタ `BrowserBlock()`
- **概要**: ブラウザブロックを初期化します。親クラス `Block` のコンストラクタに `Properties.of(Material.STONE)` を渡し、石材としてブロックを設定します。
- **呼び出し元**: Mod の初期化時にブロックが登録される際。自動的に呼び出されます。

### `InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit)`
- **概要**: プレイヤーがブロックを右クリックしたときの挙動を定義します。
- **引数**:
    - `pState` – ブロックの状態。
    - `pLevel` – 現在のワールド。
    - `pPos` – ブロックの座標。
    - `pPlayer` – インタラクトしたプレイヤー。
    - `pHand` – 使用した手 (メイン/オフハンド)。
    - `pHit` – 当たり判定の結果。
- **処理内容**:
    1. `BrowserBlockEntity be = (BrowserBlockEntity) pLevel.getBlockEntity(pPos)` で位置からブロックエンティティを取得。
    2. クライアントサイド (`pLevel.isClientSide`) の場合、`Minecraft.getInstance().setScreen(new BrowserSearchScreen(be))` を呼び出して検索画面を開く。
    3. 最終的に `super.use(...)` を呼び出して親クラスの処理を継続。
- **戻り値**: 親クラスの `use()` の戻り値 (`InteractionResult`)。
- **呼び出し元**: ゲームエンジンがプレイヤーの右クリック操作を検知したとき。

### `@Nullable BlockEntity newBlockEntity(BlockPos pPos, BlockState pState)`
- **概要**: 新しい `BrowserBlockEntity` を生成して返します。
- **引数**:
    - `pPos` – ブロックエンティティの位置。
    - `pState` – ブロックの状態。
- **戻り値**: `BrowserBlockEntity` インスタンス。
- **呼び出し元**: チャンク読み込みやブロック設置時に自動的に呼ばれます。

### `@Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType)`
- **概要**: ブロックエンティティのティック処理を登録します。
- **引数**:
    - `pLevel` – ワールド。クライアント側かサーバ側かで処理を変える。
    - `pState` – ブロック状態。
    - `pBlockEntityType` – ブロックエンティティの型。
- **処理内容**:
    1. クライアント側の場合 (`pLevel.isClientSide`) は `null` を返し、ティック処理を行わない。
    2. サーバ側の場合はラムダ式を返し、毎ティック `te instanceof BrowserBlockEntity` をチェックして `blockEntity.tickServer()` を呼び出す。
- **戻り値**: サーバサイドでは `BlockEntityTicker<T>`、クライアントサイドでは `null`。
- **呼び出し元**: ワールドの tick 処理において自動的に呼び出されます。

### `@Nullable <T extends BlockEntity> GameEventListener getListener(Level pLevel, T pBlockEntity)`
- **概要**: ゲームイベントリスナーを返すメソッド。ここでは親クラスのデフォルト実装を利用します。
- **戻り値**: デフォルトの `GameEventListener` または `null`。
- **呼び出し元**: 振動センサーなどのゲームイベントが関与する場合に呼び出されますが、このブロックでは特にカスタムリスナーを提供しません。

---

## 🔁 呼び出し関係図（関数依存）

```
Player right-click
└─ use()
├─ getBlockEntity()
├─ BrowserSearchScreen(be)  …クライアント側のみ
└─ super.use()

Chunk/Block loaded
└─ newBlockEntity()               …ブラウザブロックエンティティを生成

World tick (server side)
└─ getTicker() → BlockEntityTicker
└─ tickServer() on BrowserBlockEntity

Game events
└─ getListener()                  …親クラスのリスナーを利用
```

---

## ⚙️ 外部依存関係
| クラス／ライブラリ | 用途 |
|------------------|-----|
| **`BrowserBlockEntity`** | ブラウザブロックに対応するブロックエンティティ。ネットワーク通信やデータ転送処理を担当し、`tickServer()` が呼ばれる。 |
| **`BrowserSearchScreen`** | ブラウザ検索画面 GUI。`use()` メソッド内でクライアント側に表示される。 |
| **`ServerBlockEntity`** | インポートされているが、このクラス内では直接使用されていない。将来的な拡張や誤りの可能性。 |
| **`Minecraft`** | クライアントサイドのゲームインスタンスを取得し、画面表示を行う。 |
| **`BlockPos`, `Player`, `InteractionHand`, `BlockHitResult`** | ブロックの座標やプレイヤーのインタラクション情報を扱う。 |
| **`BlockEntity`, `BlockEntityTicker`, `GameEventListener`** | ブロックエンティティの生成・ティック・イベントリスナー管理を行う。 |
| **`Material`** | ブロックの素材設定。ここでは石材 (`STONE`) としてブロックの物性を決定する。 |

---

## 📄 ファイル別概要
| ファイル名／クラス名 | 主な責務 | 主要メソッド |
|--------------------|-----------|--------------|
| **`BrowserBlock.java`** | ブラウザブロックの本体。右クリックによる GUI 表示とブロックエンティティの生成・ティック登録を担当する。 | コンストラクタ、`use()`, `newBlockEntity()`, `getTicker()`, `getListener()` |
| **`BrowserBlockEntity.java`** | ブラウザブロックのロジックを実装。DNS リクエストや TCP ハンドシェイク、データの送受信を管理する。 | `tickServer()`, `receiveDNSResponse()`, `receiveHandshakeServerResponse()` 等 |
| **`BrowserSearchScreen.java`** | ブラウザ検索画面の GUI を実装。ユーザーから URL を受け取り、`BrowserBlockEntity` へ送信する。 | 画面描画、入力処理 |
| **`ServerBlockEntity.java`** | ブラウザからのリクエストを受け取りファイルシステムからデータを返すサーバブロックの実装。 | `receiveDataPacket()`, `tickServer()` 等 |

---

## 💬 総評
`BrowserBlock` クラスはプレイヤーインタラクションの橋渡しとしてシンプルに設計されており、状態を持たない分わかりやすく保守しやすい構造です。ブラウザ検索画面の表示やサーバサイド処理をすべて専用エンティティに委任することで責務が明確になっています。`ServerBlockEntity` のインポートが未使用である点はコード整理の余地がありますが、全体としては Mod 内のブラウザ機能のエントリーポイントとして適切な実装です。