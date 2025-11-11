# 📘 コードドキュメント

## 概要
このクラス `WifiRouterBlockEntity` は、Minecraft Mod における Wi‑Fi ルータブロックの実装であり、ケーブルと他のルータブロックを介してネットワークを構築し、DNS サーバやアプリケーションサーバとの通信を仲介します。主な役割は、**パケットのルーティング**・**DNS リクエストの転送**・**サーバリクエストの転送**・**ネットワークの自動構築**を行うことです。各ルータは自分の位置と周囲のケーブル・ルータの情報をもとにグラフを構築し、最短経路探索によりパケットを目的地へ送ります。主な処理の流れは以下の通りです。

1. **起動時処理** (`onLoad()` → `loadDNSServers()`)：世界に登録されている DNS サーバを探索し、ルータネットワークを構築 (`connectToRouterNetwork()`)。最寄りの DNS サーバブロック位置を取得し `dnsServerBlockPos` に保持する。
2. **DNS リクエストの送信** (`performDNSRequest()`)：ブラウザから渡された URL 文字列を DNS サーバに送る。ルータグラフから DNS サーバへの最短経路とケーブルパスを算出し、`DataPacket` を作成して送信キューに登録する。
3. **サーバリクエストの送信** (`performServerRequest()`)：サーバブロックへの接続要求を処理する。ターゲットサーバに最も近いルータを選択し、最短経路をたどってパケットを送る。
4. **パケット転送管理** (`tickServer()`)：送信キューを監視し、空いているケーブルがあれば次の `DataPacket` を送信する。送信完了を検知したら状態をリセットする。
5. **受信パケット処理** (`receiveTransmittedPacket()`)：ケーブルから受信したパケットを解析し、次のルータへ転送するか、最終ルータであれば DNS サーバ・サーバブロック・ブラウザへ渡す。誤配送の場合は再度リクエストを生成して転送する。
6. **ネットワーク探索** (`connectToRouterNetwork()`)：BFS を用いて周囲のルータとケーブルを探索し、`Graph` にノードとエッジを登録する。ルータ同士を直接結ぶケーブル距離も記録し、ネットワーク全体を可視化できるようにする。

---

## 🌐 グローバル変数一覧
| 変数名 | 型 | 初期値 | 使用箇所 | 説明 |
|--------|----|-------|-------------|------|
| `routerMap` | `Graph` | `new Graph()` | `loadDNSServers()`, `performDNSRequest()`, `performServerRequest()`, `connectToRouterNetwork()` など | ルータ間の接続を表すグラフ構造。ノードはルータ位置、エッジはケーブル本数（距離）を表す。最短経路探索や DNS サーバ位置探索に用いる。 |
| `dnsServerBlockPos` | `BlockPos` | `null` | `loadDNSServers()`, `performDNSRequest()`, `isConnectedToDNSServer()` | 最近接の DNS サーバブロックの位置を保持する。DNS リクエスト送信時に参照される。 |
| `currentPos` | `BlockPos` | コンストラクタ引数でセット | 多数 | このルータブロックのワールド座標。経路探索やハッシュキーとして利用される。 |
| `readyToTransmit` | `boolean` | `false` | `tickServer()`, `resetTransmissionState()` | 送信中のパケットがあるかどうかを示すフラグ。`true` の間は新しい送信を開始しない。 |
| `currentPacket` | `DataPacket` | `null` | `tickServer()` | 現在送信中のデータパケットを保持する。送信完了後に `null` に戻される。 |
| `currentTargetCable` | `CableBlockEntity` | `null` | `tickServer()` | 現在送信対象となっているケーブルブロック。送信完了を検知するために参照される。 |
| `transmissionQueue` | `Queue<TransmissionTask>` | `new LinkedList<>()` | `queueTransmission()`, `tickServer()` | 送信待機中のタスクを FIFO で保持するキュー。各タスクはパケットと送信先ケーブルを含む。 |
| `MAX_DEPTH` | `int` | `3` (定数) | `connectToRouterNetwork()` | BFS での探索深さの上限。無限ループ防止のためネットワーク探索を 3 段までに制限する。 |
| `worldPosition` | `BlockPos` | 親クラスより | `loadDNSServers()`, `performDNSRequest()`, `performServerRequest()` | ブロックエンティティの絶対位置。現在地として `currentPos` と同じ値を返す。 |
| `level` | `Level` | 親クラスより | 多数 | ワールドコンテキスト。クライアント／サーバ判定やブロックエンティティ取得、ブロック設置に使用される。 |

---

## 🧩 関数一覧

### コンストラクタ `WifiRouterBlockEntity(BlockPos pWorldPosition, BlockState pBlockState)`
- **概要**: ルータブロックエンティティを生成し、現在位置 (`currentPos`) を初期化する。
- **引数**: `pWorldPosition` – ブロック位置、`pBlockState` – ブロック状態。親クラス `BlockEntity` のコンストラクタに `ExampleMod.WIFI_ROUTER_ENTITY` を渡して登録する。
- **呼び出し元**: ルータブロックがワールドに配置されたときに自動生成される。

### `onLoad() -> void`
- **概要**: ブロックエンティティがワールドにロードされた際に呼び出され、DNS サーバ探索およびネットワーク構築を開始する。
- **処理内容**: `super.onLoad()` を呼び出した後 `loadDNSServers()` を実行して周辺ネットワークをスキャンし DNS サーバを登録する。
- **呼び出し元**: Minecraft のブロック読み込み処理。

### `loadDNSServers() -> void`
- **概要**: サーバレベル内の DNS サーバを探索し、最も近い DNS サーバブロックを設定。あわせてルータネットワークを可視化する。
- **主な処理**:
    1. `connectToRouterNetwork(currentPos)` を呼び出し、深さ 3 までのルータとケーブルを探索してグラフに登録。
    2. `DnsServerSavedData.get(serverLevel)` から DNS サーバリストを取得し、空でなければ `getNearestDNSServerBlockPosition()` により最近接の DNS サーバブロックを決定して `dnsServerBlockPos` に保存。
    3. `routerMap.visualizeNetwork(serverLevel, Blocks.AMETHYST_BLOCK)` を呼び、ネットワーク構造をアメジストブロックで可視化（デバッグ用）。
- **呼び出し元**: `onLoad()`, `performDNSRequest()`。

### `getNearestDNSServerBlockPosition(List<BlockPos> positions, BlockPos routerPos) -> BlockPos?`
- **概要**: 複数の DNS サーバブロック候補から最も近く、かつルータに接続されている DNS サーバを選択する。利用可能な DNS サーバが無い場合は単純に距離で最も近いブロックを返す。
- **処理内容**:
    1. `positions` からすぐ下にルータが存在する DNS サーバをフィルタ。
    2. フィルタ結果が空なら単純に `routerPos` からの距離で最も近い位置を返す。
    3. ルータを持つ DNS サーバについて、`BlockPos.below()` でその下のルータ位置を取得し、`routerMap.findClosestTarget()` で現在位置から到達可能な最も近いルータを求める。
    4. そのルータの上にあるブロック (`BlockPos.above()`) を DNS サーバブロックとして返す。
- **呼び出し元**: `loadDNSServers()`。

### `performDNSRequest(byte[] urlBytes, List<BlockPos> oldPath, BlockPos clientPos) -> void`
- **概要**: ブラウザからの URL バイト列を DNS サーバに送るためのリクエストパケットを作成し、送信キューに登録する。
- **主な処理**:
    1. クライアント側かどうかを判定し、クライアントサイドなら処理を終了。
    2. `loadDNSServers()` を呼び、最新の DNS サーバおよびネットワークを読み込む。`isConnectedToDNSServer()` が `false` の場合はエラーログを出して終了。
    3. URL バイト列を `String` 化してログ出力。DNS サーバのルータ位置 (`dnsServerBlockPos.below()`) を取得し、その位置にルータが存在するかを確認。
    4. ルータグラフから DNS サーバに最も近いルータ (`nearestRouter`) を検索し、`routerMap.findShortestPath(currentPos, nearestRouter)` で最短ルータ経路を求める。
    5. 経路が存在しなければエラー。存在する場合は `visualizePathWithBlocks()` と `visualizeNextRouterWithRedstone()` でデバッグ用に表示。
    6. 現在ルータ (`currentPos`) から次のルータへと接続するケーブルブロックリストを `findCablePathBetweenRouters()` で取得。
    7. DNS リクエスト `DataPacket` を作成し、`oldPath` が指定されていればそれをマージ（`addNewRouterPathToOldPath()`）してルータ経路を更新。
    8. ケーブルの先頭ブロックに存在する `CableBlockEntity` を取得し、`queueTransmission()` により送信キューに登録する。
- **呼び出し元**: ブラウザブロックからの DNS リクエスト処理や DNS 応答の再送時。`receiveTransmittedPacket()` で DNS ルータではないルータにパケットが到達した場合に呼び出される。

### `tickServer(Level level) -> void`
- **概要**: サーバ側で毎ティック呼び出され、送信キューの管理と実際のパケット送信を担当する。
- **主な処理**:
    1. クライアントサイドの場合は処理を行わない。
    2. 送信中でない (`readyToTransmit == false`) かつ送信キューが空でない場合は次の `TransmissionTask` を取り出す。送信先ケーブルがデータを保持していなければ (`!target.hasDataPacket()`)、`enqueuePacket()` を呼んでパケットをケーブルに投入し、送信状態を更新する。ケーブルが使用中の場合は再度キューに戻す。
    3. 送信中 (`readyToTransmit == true`) かつ `currentTargetCable` がデータを保持していない（送信完了）の場合は `resetTransmissionState()` を呼び、次の送信準備に移行する。
- **呼び出し元**: ルータブロックの毎ティック処理。
- **呼び出し先**: `enqueuePacket()`（ケーブルブロックへの送信）, `resetTransmissionState()`。

### `resetTransmissionState() -> void`
- **概要**: 現在の送信状態をリセットし、新たな送信を受け付けられるようにする。
- **処理内容**: `readyToTransmit = false`、`currentPacket = null`、`currentTargetCable = null`。
- **呼び出し元**: `tickServer()`。

### `queueTransmission(DataPacket packet, CableBlockEntity cable) -> void`
- **概要**: 新しい送信タスク（データパケット + 送信先ケーブル）をキューに追加する。
- **引数**: `packet` – 送信すべき `DataPacket`。`cable` – 送信先の `CableBlockEntity`。いずれかが `null` の場合は何もしない。
- **処理内容**: `TransmissionTask` を生成し `transmissionQueue.offer()` でキューに追加。ログ出力あり。
- **呼び出し元**: `performDNSRequest()`, `performServerRequest()`, `transmitPacket()`, `receiveTransmittedPacket()` など。

### `receiveTransmittedPacket(DataPacket packet, CableBlockEntity cableBlockEntity) -> void`
- **概要**: ケーブルブロックから受信したパケットを処理し、最終ルータなら DNS サーバ／アプリケーションサーバ／ブラウザに渡し、それ以外の場合は次のルータへ転送する。
- **主な処理**:
    1. パケットが `null` なら終了。
    2. `cableBlockEntity.setSenderToNull()` で送信者メタデータをクリア（旧 API の互換性のため）。
    3. `packet.getRouterPath()` から残りのルータ経路を取得。空であればエラーとして終了。
    4. `getNextRouterBlockPos(routerPath)` により次のルータ位置を取得。`null` の場合は最終ルータであり、以下のように分岐：
        - **リクエストパケット** (`packet.getErrorCode() == null`) の場合：
            - `packet.getQueryType()` が `DNS` なら DNS 要求。もしこのルータが DNS サーバに接続されていない (`!isDNSServerRouter()`) 場合は `performDNSRequest()` で再ルーティング。そうでなければ真上の `DNSServerBlockEntity` に `receiveDataPacket()` を渡す。
            - `TCP_HANDSHAKE`, `TCP_ESTABLISHED`, `TCP_DISCONNECT` の場合はアプリケーションサーバへのリクエスト。`!isServerRouter()` なら `performServerRequest()` で最寄りサーバへルーティング。そうでなければ真上の `ServerBlockEntity` にパケットを渡す。
        - **応答パケット** (エラーコード有り) の場合：クライアント位置 `packet.getClientBlockPos()` から `BrowserBlockEntity` を取得し、クエリタイプに応じて `receiveDNSResponse()`, `receiveHandshakeServerResponse()`, `receiveEstablishedServerResponse()`, `receiveDisconnectServerResponse()` を呼び出してブラウザへ返す。
    5. 次のルータが存在する場合はケーブル経路を更新し (`findCablePathBetweenRouters()`)、新しい `DataPacket` を生成して次のケーブルへ送る (`queueTransmission()`)。
- **呼び出し元**: `CableBlockEntity` の `tickServer()` がパケットをルータに引き渡した際。
- **呼び出し先**: `performDNSRequest()`, `performServerRequest()`, 各種ブロックエンティティの `receiveDataPacket()`、`queueTransmission()` など。

### `performServerRequest(byte[] filePathBytes, BlockPos serverIPAddress, List<BlockPos> oldRouterPath, BlockPos clientPos, Queries queryType) -> void`
- **概要**: サーバブロックへのリクエストを生成し、ルータ経路およびケーブル経路を設定して送信キューに登録する。ファイルパスやクエリタイプ (`TCP_HANDSHAKE`, `TCP_ESTABLISHED`, `TCP_DISCONNECT`) を指定できる。
- **主な処理**:
    1. クライアントサイド判定を行い、クライアントなら終了。
    2. サーバブロックの真下のルータ位置 (`serverIPAddress.below()`) を `routerMap` に含むか調べ、含まれていなければネットワーク内で最も近いルータを `sendToRouter` とする。
    3. 指定したルータ位置に実際にルータブロックが存在するか確認 (`hasRouterAtPosition()`); 無ければ最も近いルータへの送信を試みる。
    4. `routerMap.findShortestPath(currentPos, sendToRouter)` で最短ルータ経路を求め、`getNextRouterBlockPos()` で次のルータ位置を取得。
    5. `findCablePathBetweenRouters()` でケーブル経路を求め、`DataPacket` を組み立てる。旧経路 (`oldRouterPath`) が指定されていれば `addNewRouterPathToOldPath()` で接続経路を結合する。
    6. 先頭ケーブルブロックから `CableBlockEntity` を取得し、`queueTransmission()` で送信キューに追加。
- **呼び出し元**: `receiveTransmittedPacket()` がサーバリクエストを再ルーティングする場合や、ブラウザから直接サーバへアクセスする場合。

### `transmitPacket(DataPacket dataPacket) -> void`
- **概要**: 外部から明示的にパケット送信を要求されたときに呼び出され、ケーブル経路が正しく設定されていれば送信キューに登録する。
- **処理内容**: `dataPacket.getCablePath()` を取り出し、先頭ケーブルブロックが `CableBlockEntity` であることを確認。問題がなければ `queueTransmission(dataPacket, cableBlockEntity)` を呼び出す。
- **呼び出し元**: DNS サーバやサーバブロックが応答パケットを返す際など。

### `safeGetBlockEntity(ServerLevel serverLevel, BlockPos pos) -> BlockEntity`
- **概要**: 指定位置のチャンクが未読み込みであれば強制的に読み込み、そのブロックエンティティを取得する。
- **処理内容**: `serverLevel.hasChunkAt(pos)` でチャンクが読み込まれているか確認し、読み込まれていなければ `serverLevel.getChunkAt(pos)` で読み込み、`getBlockEntity(pos)` を返す。
- **呼び出し元**: `receiveTransmittedPacket()` の最終処理で DNS サーバやサーバブロック、ブラウザブロックを取得する際。

### `static findCablePathBetweenRouters(Level level, BlockPos startRouter, BlockPos endRouter) -> List<BlockPos>`
- **概要**: 2 つのルータ間でケーブルのみを通って繋がっている経路を BFS で探索し、そのケーブルブロックのリストを返す。開始・終了のルータ自身は含めない。
- **処理内容**:
    1. BFS を用いてケーブルブロックと終端ルータ (`endRouter`) を探索。`previous` マップに前のノードを記録していく。
    2. `current.equals(endRouter)` で探索終了したら、`previous` を辿ってケーブルパスを構築し、ケーブルブロックのみをリストに追加する。
    3. 見つかったケーブルパスのリストを逆順に整えて返す。
- **呼び出し元**: `performDNSRequest()`, `performServerRequest()`, `receiveTransmittedPacket()`。

### `@Nullable getNextRouterBlockPos(List<BlockPos> path) -> BlockPos?`
- **概要**: 現在のルータ (`currentPos`) が含まれるルータ経路リストから、次に到達すべきルータ位置を取得する。
- **処理内容**: `path.indexOf(currentPos)` で現在ルータのインデックスを取得し、末尾でなければ次の要素を返す。見つからない、または末尾の場合は `null` を返す。
- **呼び出し元**: `performDNSRequest()`, `performServerRequest()`, `receiveTransmittedPacket()`。

### `visualizeNextRouterWithRedstone(List<BlockPos> path) -> void`
- **概要**: デバッグ用に、次のルータ位置の数ブロック上にレッドストーンブロックを置いて可視化する。クライアントサイドでは何もしない。
- **呼び出し元**: `performDNSRequest()`。

### `visualizePathWithBlocks(List<BlockPos> path, Block markerBlock) -> void`
- **概要**: ルータ経路をデバッグ用に可視化する。経路の各ルータ位置の 2 ブロック上に指定のブロック（例：グロウストーン）を設置する。
- **呼び出し元**: `performDNSRequest()`。

### `isConnectedToDNSServer() -> boolean`
- **概要**: DNS サーバブロックが登録されているかどうかをチェックする。
- **処理内容**: `dnsServerBlockPos != null` を返す。
- **呼び出し元**: `performDNSRequest()`。

### `hasRouterAtPosition(BlockPos pos) -> boolean`
- **概要**: 指定した位置に Wi‑Fi ルータブロックエンティティが存在するかを確認する。
- **処理内容**: `level.getBlockEntity(pos) instanceof WifiRouterBlockEntity` で判定。
- **呼び出し元**: `getNearestDNSServerBlockPosition()`, `performDNSRequest()`, `performServerRequest()` など。

### `connectToRouterNetwork(BlockPos startPos) -> void`
- **概要**: 周囲のルータとケーブルを探索してネットワークグラフを構築する。深さ制限付き BFS を用いてルータを発見し、それぞれを `routerMap` に追加・接続する。
- **主な処理**:
    1. 初期ルータ (`startPos`) をグラフに追加し、`RouterNode(startPos, 0)` をキューに入れる。
    2. BFS を行い、`current.depth` が `MAX_DEPTH` より小さい場合のみ以下を処理：
        - `findConnectedRouters(level, current.pos)` でケーブル経由で到達可能な隣接ルータとそのケーブル長を取得。
        - 各隣接ルータをグラフに追加し、現在のルータとエッジで接続する (`routerMap.addConnection()`)。
        - 未探索であればキューに追加し、探索済み集合に登録する。
    3. BFS 完了後、ネットワーク内のすべてのルータペアに対し、ケーブル距離 (`getCableDistance()`) が存在すればエッジを追加し完全な連結を保証する。
    4. 最終的なルータ数をログ出力する。
- **呼び出し元**: `loadDNSServers()`。

### `getCableDistance(Level level, BlockPos start, BlockPos end) -> int`
- **概要**: 2 つのルータ間のケーブル距離（ケーブル本数）を BFS で探索する。ルータ間に直接ケーブル接続が存在しない場合は -1 を返す。
- **呼び出し元**: `connectToRouterNetwork()` の2回目のルータ接続処理。

### `findConnectedRouters(Level level, BlockPos startRouter) -> Map<BlockPos, Integer>`
- **概要**: 指定したルータからケーブル経由で到達できる隣接ルータや DNS サーバを探索し、それらの距離（ケーブル本数）をマップとして返す。
- **処理内容**: BFS を行い、隣接ブロックがケーブルの場合は深さを 1 増やして進み、隣接ブロックがルータ (`WifiRouterBlockEntity`) または DNS サーバ (`DNSServerBlockEntity`) であれば発見として `foundRouters.put(neighbor, distance)` に登録する。
- **呼び出し元**: `connectToRouterNetwork()`。

### `isValidNode(BlockEntity be) -> boolean`
- **概要**: 指定したブロックエンティティがルータもしくは DNS サーバブロックであるかを判定する。
- **処理内容**: `be instanceof WifiRouterBlockEntity || be instanceof DNSServerBlockEntity` を返す。
- **呼び出し元**: `findConnectedRouters()`。

### `static boolean isCableBlock(Block block) -> boolean`
- **概要**: 指定したブロックがケーブルブロックかどうかを確認する。
- **処理内容**: `block.equals(ExampleMod.CABLE_BLOCK)` を返す。
- **呼び出し元**: `findCablePathBetweenRouters()`, `connectToRouterNetwork()`, `getCableDistance()` など。

### `static BlockPos[] getNeighbors(BlockPos pos) -> BlockPos[]`
- **概要**: 与えられた位置の6方向（上・下・北・南・東・西）の隣接座標を返す。ネットワーク探索用のヘルパ。
- **呼び出し元**: `findCablePathBetweenRouters()`, `getCableDistance()`, `findConnectedRouters()`。

### `Graph getRouterMap() -> Graph`
- **概要**: 内部で保持しているルータグラフを返す。主に外部モジュールがネットワーク構造を参照するために使用する。
- **呼び出し元**: 診断ツールやデバッグ用コード。

### `isDNSServerRouter() -> boolean`, `isServerRouter() -> boolean`
- **概要**: 現在のルータブロックの真上のブロックが DNS サーバブロック (`ExampleMod.DNSSERVER_BLOCK`) またはサーバブロック (`ExampleMod.SERVER_BLOCK`) であるかどうかを判定する。
- **処理内容**: `level.getBlockState(currentPos.above()).getBlock().equals(...)` で比較し、該当すれば `true` を返す。
- **呼び出し元**: `receiveTransmittedPacket()`。

### 内部クラス `RouterNode` / `PathNode`
- **概要**: BFS 探索時に使用するノードの座標と深さ／距離を保持するレコードクラス。
- **使用箇所**: `connectToRouterNetwork()`, `getCableDistance()`, `findConnectedRouters()`。

---

## 🔁 呼び出し関係図（関数依存）
Wi‑Fi ルータブロック内の主要な関数の呼び出し関係を以下に示します。
```
onLoad()
└─ loadDNSServers()
├─ connectToRouterNetwork()
├─ getNearestDNSServerBlockPosition()
└─ routerMap.visualizeNetwork()

performDNSRequest()
├─ loadDNSServers()
├─ getNearestDNSServerBlockPosition()
├─ routerMap.findShortestPath()
├─ visualizePathWithBlocks()
├─ visualizeNextRouterWithRedstone()
├─ getNextRouterBlockPos()
├─ findCablePathBetweenRouters()
└─ queueTransmission()

tickServer()
├─ queueTransmission()  (新規送信)
├─ enqueuePacket()      (CableBlockEntity 内部)
└─ resetTransmissionState()

receiveTransmittedPacket()
├─ cableBlockEntity.setSenderToNull()
├─ getNextRouterBlockPos()
├─ performDNSRequest()       (再ルーティング)
├─ performServerRequest()    (再ルーティング)
├─ DNSServerBlockEntity.receiveDataPacket()  (DNS 処理)
├─ ServerBlockEntity.receiveDataPacket()     (サーバ処理)
├─ BrowserBlockEntity.receive*()             (ブラウザ処理)
├─ findCablePathBetweenRouters()
└─ queueTransmission()

performServerRequest()
├─ hasRouterAtPosition()
├─ routerMap.findShortestPath()
├─ getNextRouterBlockPos()
├─ findCablePathBetweenRouters()
└─ queueTransmission()

connectToRouterNetwork()
├─ findConnectedRouters()
├─ routerMap.addRouter()
├─ routerMap.addConnection()
├─ getCableDistance()
└─ routerMap.addConnection()  (二回目の結合)
```
---

## ⚙️ 外部依存関係
| クラス／ライブラリ | 用途と理由 |
|------------------|-----------|
| **`Graph`** | ルータ間の接続関係を表すグラフデータ構造。最短経路探索や接続の追加 (`addRouter()`, `addConnection()`) に利用される。 |
| **`DnsServerSavedData`** | サーバワールド内に登録された DNS サーバブロックの位置を保持するデータクラス。`getDnsServers()` で一覧を取得できる。 |
| **`DataPacket`** | ネットワークパケットを表すクラス。ルータ経路・ケーブル経路・クエリタイプ・送信元／宛先位置などを保持し、`addNewRouterPathToOldPath()` や `updateCablePath()` などで経路更新が可能。 |
| **`Queries`** | パケットの種類を示す列挙型。`DNS`, `TCP_HANDSHAKE`, `TCP_ESTABLISHED`, `TCP_DISCONNECT` など。 |
| **`Senders`** | 送信者を表す列挙型。ルータがケーブルにパケットを enqueue する際に `Senders.ROUTER` を指定する。 |
| **`BrowserBlockEntity`**, **`DNSServerBlockEntity`**, **`ServerBlockEntity`** | 最終処理を行うエンティティ。ルータは最終ルータに到達したパケットをこれらのブロックに渡し、DNS 応答・サーバ応答・ブラウザ応答を処理する。 |
| **`CableBlockEntity`** | ケーブルブロックエンティティ。`enqueuePacket()` でパケットを待機キューに入れ、`hasDataPacket()` で処理中かどうかを判断する。ルータはケーブルを経由して隣接ルータへパケットを送る。 |
| **`DnsServerSavedData`**, **`ServerLevel`** | DNS サーバ探索やチャンク読み込みを行うために使用。`safeGetBlockEntity()` で未読み込みチャンクを強制ロードする。 |
| **`BlockPos`, `BlockState`, `BlockEntity`, `Blocks`** | Minecraft の基本クラス。ブロック位置や状態、ブロック種類（例：ケーブルブロックやデバッグ用マーカー）を扱う。 |

---

## 📄 ファイル別概要
| ファイル／クラス名 | 主な責務 | 主な関数 |
|------------------|-----------|-----------|
| **`WifiRouterBlockEntity.java`** | ルータブロックの実装。DNS サーバやサーバブロックとの間でパケットをルーティングし、ケーブル経路を探索してパケットを中継する。ネットワークグラフの構築や可視化も担当する。 | コンストラクタ、`onLoad()`, `loadDNSServers()`, `performDNSRequest()`, `performServerRequest()`, `tickServer()`, `receiveTransmittedPacket()`, `connectToRouterNetwork()` など |
| **`Graph.java`** | ルータノードと接続エッジを管理するグラフ構造。ルータ追加・接続追加・最短経路探索等のメソッドを提供する。 | `addRouter()`, `addConnection()`, `findShortestPath()`, `findClosestTarget()`, `visualizeNetwork()` |
| **`CableBlockEntity.java`** | ケーブルブロックエンティティ。パケットをキューに追加 (`enqueuePacket()`)、現在処理中のパケットを確認 (`hasDataPacket()`) する役割を担う。 | `enqueuePacket()`, `tickServer()` 等 |
| **`BrowserBlockEntity.java`**, **`DNSServerBlockEntity.java`**, **`ServerBlockEntity.java`** | それぞれブラウザ、DNS サーバ、アプリケーションサーバの処理を担当するブロックエンティティ。ルータから送られたパケットを最終処理する。 | `receiveDNSResponse()`, `receiveHandshakeServerResponse()`, `receiveEstablishedServerResponse()`, `receiveDisconnectServerResponse()`, `receiveDataPacket()` など |

---

## 💬 総評
`WifiRouterBlockEntity` は、仮想ネットワークのルータとして高度なルーティング機能を実装しており、周囲のケーブルとルータを探索してグラフを生成し、最短経路に従ってパケットを転送します。DNS サーバやサーバブロックとの通信だけでなく、ネットワークの拡張や冗長経路の解決にも対応しており、パケットが誤ったルータに到達した場合でも適切に再ルーティングされます。また、デバッグ用の可視化機能（グロウストーン・レッドストーンによるマーク）を備え、ネットワーク構造の理解を助けます。

改善点としては、`transmissionQueue` の再試行ロジックや BFS 深度の調整によるパフォーマンス最適化が挙げられます。エラーログが多数標準出力に出力されているため、GUI 連携やログレベル制御といった拡張も検討できます。それでも、本コードはネットワークシミュレーションの中核として堅牢に設計されており、教育的な題材としても有益です。