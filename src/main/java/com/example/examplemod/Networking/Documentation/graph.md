# コードドキュメント

## 概要
`Graph` クラスは、Minecraft Mod 内で使用されるルータネットワークを表現するデータ構造です。各ルータの位置をノードとし、ノード間のケーブルの本数を重みとして保持する無向グラフを実装しています。本クラスはネットワーク探索や可視化に関するさまざまな機能を提供し、ルータブロック同士の最短経路を求めたり、接続をデバッグする際の補助を行います。主な処理の流れは以下のとおりです。

- ルータをグラフに追加し、ルータ間の接続（エッジ）を追加する。
- Dijkstra 法を用いて、二つのルータ間の最短経路を計算する。
- ネットワーク全体をワールド内で可視化するために、各ルータの上にマーカーを設置する。
- 複数のターゲットルータの中から、指定した出発ルータに最も近いルータを探索する。

---

## グローバル変数一覧
| 変数名 | 型 | 初期値 | 使用箇所 | 説明 |
|--------|----|--------|----------|------|
| `adjacencyList` | `Map<BlockPos, Map<BlockPos, Integer>>` | 空の `HashMap` | 全メソッド | 各ルータ位置 (`BlockPos`) をキーとし、隣接ルータとその距離（ケーブル本数）を格納する内部グラフ表現。 |

---

## 関数一覧

### `void addRouter(BlockPos routerPos)`
- **概要**: 新しいルータノードをグラフに追加します。既に存在する場合は無視します。
- **引数**: `routerPos` – ルータの位置。
- **呼び出し元**: ルータブロックの配置時やネットワーク探索の初期化時。

### `void addConnection(BlockPos fromRouter, BlockPos toRouter, int cost)`
- **概要**: 二つのルータ間に無向エッジを追加し、指定された重み（ケーブル距離）を設定します。
- **引数**: `fromRouter`, `toRouter` – 接続するルータの位置。`cost` – 接続にかかるケーブル本数。
- **呼び出し元**: ルータネットワーク構築時に使用。

### `Map<BlockPos, Integer> getConnections(BlockPos routerPos)`
- **概要**: 指定されたルータに隣接するルータとその距離を返します。
- **戻り値**: 隣接ルータとケーブル距離を保持するマップ。

### `Set<BlockPos> getRouters()`
- **概要**: グラフに登録されているすべてのルータ位置の集合を返します。

### `List<BlockPos> findShortestPath(BlockPos start, BlockPos end)`
- **概要**: Dijkstra アルゴリズムを用いて、`start` から `end` までの最短経路を求めます。
- **処理内容**:
    1. 全ルータに対して初期距離を `Integer.MAX_VALUE` に設定し、開始ノードの距離を 0 とする。
    2. 優先度付きキューで未訪問ノードを管理し、最小距離のノードから距離を更新。
    3. 終点に到達するか、キューが空になるまで探索を続ける。
    4. `previous` マップを辿って経路を復元し、逆順にしてリストとして返す。
- **戻り値**: 最短経路のリスト。経路が存在しない場合は空リスト。
- **呼び出し元**: ルータブロックの経路探索 (`WifiRouterBlockEntity.performDNSRequest()` など)。

### `void visualizeNetwork(Level level, Block markerBlock)`
- **概要**: デバッグ目的でネットワークグラフを可視化します。各ルータの 2 ブロック上に指定したマーカーブロックを設置します。
- **引数**: `level` – 実行中のワールド。クライアント側では実行しません。  
  `markerBlock` – 可視化に使用するブロック（例: アメジストブロック）。
- **処理内容**:
    1. サーバサイドであることを確認。
    2. 各ルータ位置の上 2 ブロック目に空きがあればマーカーを設置し、ログを出力。
    3. 全マーカー設置後に完了メッセージをログに出力。
- **呼び出し元**: プレイヤーがルータブロックを右クリックした際 (`WifiRouterBlock.use()`) など。

### `@Nullable BlockPos findClosestTarget(BlockPos start, List<BlockPos> targets)`
- **概要**: 指定した出発ルータから複数のターゲットルータの中で最も近いものを探索します。
- **処理内容**:
    1. Dijkstra アルゴリズムの一部を応用し、最短距離候補を更新しながらターゲットリストに含まれるノードを探す。
    2. ターゲットの中で最小距離のノードを見つけた時点で探索を終了し、そのノードを返す。
    3. ルータが見つからない場合は `null` を返す。
- **呼び出し元**: ルータブロックが DNS サーバやサーバブロックを最も近いルータに接続する際 (`WifiRouterBlockEntity.getNearestDNSServerBlockPosition()` など)。

---

## 呼び出し関係図（関数依存）
```
Graph.addRouter() / Graph.addConnection()
└─ adjacencyList 更新

findShortestPath()
├─ adjacencyList.keySet()
├─ getConnections() …隣接ノード取得
└─ 緩和操作と previous マップ更新 → path 復元

visualizeNetwork()
├─ adjacencyList.keySet()
└─ level.setBlock()  …デバッグ用マーカー設置

findClosestTarget()
├─ adjacencyList.keySet()
├─ getConnections()
└─ Dijkstra ベース探索 …ターゲット候補の最短距離探索
```

---

## 外部依存関係
| クラス／ライブラリ | 用途 |
|------------------|----|
| **`BlockPos` (Minecraft API)** | ルータやケーブルブロックの座標を表す。グラフのノードに使用。 |
| **`Level` (Minecraft API)** | ワールド情報を取得し、ブロックの設置やサーバ／クライアント判定に使用。 |
| **`Block` (Minecraft API)** | 可視化に使用するマーカーブロック。 |
| **`java.util` パッケージ** | `Map`, `List`, `Set`, `PriorityQueue` など、グラフデータ構造や探索アルゴリズムに使用。 |
| **`@Nullable` (javax.annotation)** | 戻り値が null になり得ることを明示するアノテーション。 |

---

## ファイル別概要
| ファイル名／クラス名 | 主な責務 | 主なメソッド |
|--------------------|-----------|--------------|
| **`Graph.java`** | ルータネットワークのグラフ構造を管理し、最短経路探索・ターゲット探索・可視化を提供する。 | `addRouter()`, `addConnection()`, `getRouters()`, `findShortestPath()`, `visualizeNetwork()`, `findClosestTarget()` |
