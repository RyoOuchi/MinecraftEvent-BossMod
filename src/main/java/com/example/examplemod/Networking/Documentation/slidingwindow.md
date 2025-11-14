# コードドキュメント

## 概要
`SlidingWindow` クラスは、ネットワーク通信のフロー制御に用いられるスライディングウィンドウ方式を実装したユーティリティです。送信キューに積まれたデータパケットをウィンドウサイズに応じて送り出し、ACK を受け取ることでウィンドウを進め、タイムアウトしたパケットを再送する機能を提供します。これにより信頼性の高いデータ転送を実現します。

---

## グローバル変数一覧
`SlidingWindow` クラスはインスタンスフィールドとして以下の変数を保持します。クラスレベルでの静的変数は存在しません。

| 変数名 | 型 | 初期値 | 使用箇所 | 説明 |
|--------|----|--------|----------|------|
| `DEFAULT_WINDOW_SIZE` | `int` | `3` | 定数 | デフォルトのウィンドウサイズを示す定数。 |
| `DEFAULT_TIMEOUT_MILLIS` | `int` | `3000` | 定数 | パケット送信後に ACK を待つタイムアウト時間（ミリ秒）。 |
| `MAX_DATA_SIZE` | `int` | `8` | 定数 | 一度に送信するデータチャンクの最大バイト数。 |
| `windowSize` | `int` | コンストラクタ引数 | `getWindowSize()` | 現在のスライディングウィンドウの最大許容サイズ。 |
| `sendQueue` | `Queue<DataPacket>` | 空の `LinkedList` | `queueData()`, `getPacketsToSend()` | 送信待ちの `DataPacket` を格納するキュー。 |
| `inFlight` | `Map<Integer, TimedPacket>` | 空の `HashMap` | `getPacketsToSend()`, `acknowledge()`, `getUnacknowledgedPackets()`, `getTimedOutPackets()` | 送信済みだが ACK がまだ返ってきていないパケットを、シーケンス番号をキーとして保持する。 |
| `nextSeqNumber` | `int` | コンストラクタ引数 `initialSeq` | `getPacketsToSend()` | 次に送信するパケットのシーケンス番号基点。ペイロード長に応じて更新される。 |
| `lastAckedSeq` | `int` | コンストラクタ引数 `initialSeq` | `acknowledge()`, `getLastAckedSeq()` | 最後に ACK を受信したシーケンス番号。 |

内部クラス `TimedPacket` には以下のフィールドがあります。
| フィールド名 | 型 | 説明 |
|--------------|----|------|
| `packet` | `DataPacket` | 実際に送信されたデータパケット。 |
| `lastSentTime` | `long` | 最後に送信した時刻 (ミリ秒)。タイムアウト判定に使用。 |

---

## 関数一覧

### コンストラクタ `SlidingWindow(int initialSeq, int windowSize)`
- **概要**: スライディングウィンドウの初期状態を設定します。
- **引数**:
    - `initialSeq` – 初期シーケンス番号（ACK 基点）。
    - `windowSize` – ウィンドウのサイズ（同時に送信できるパケット数の上限）。
- **処理内容**: `nextSeqNumber` と `lastAckedSeq` を `initialSeq` に設定し、`windowSize` をセットします。
- **呼び出し元**: クライアントやサーバがハンドシェイク完了後にウィンドウを初期化する際 (`BrowserBlockEntity.receiveHandshakeServerResponse()`、`ServerBlockEntity.handleHandshake()` など)。

### `void queueData(DataPacket packet)`
- **概要**: 送信待ちパケットキューに `DataPacket` を追加します。
- **引数**: `packet` – キューに追加するデータパケット。
- **呼び出し元**: データ送信準備時 (`sendUrlData()`, `sendFileData()` など)。

### `List<DataPacket> getPacketsToSend()`
- **概要**: キューからウィンドウサイズの空き分だけパケットを取り出し、送信対象として返します。同時に `inFlight` マップに登録します。
- **処理内容**:
    1. `while` ループで `sendQueue` が空でないかつ `inFlight` のサイズがウィンドウサイズ未満である間、パケットを取り出す。
    2. `Message` と `Body` を使ってペイロード長を取得し、`inFlight.put(nextSeqNumber, new TimedPacket(packet))` で登録。
    3. `nextSeqNumber += payloadLen` として次に送るべきシーケンス番号を更新。
    4. 取り出したパケットを `ready` リストに追加。
- **戻り値**: 送信可能なパケットのリスト。
- **呼び出し元**: `ServerBlockEntity.handleSendPhase()`, `BrowserBlockEntity.handleEstablishedAckSegment()` など、送信機会があるタイミング。

### `void acknowledge(int ackNumber)`
- **概要**: 受け取った ACK 番号に基づいて `inFlight` から送信済みパケットを削除し、最後に ACK されたシーケンス番号を更新します。
- **引数**: `ackNumber` – 受信した ACK 番号。
- **処理内容**:
    1. `inFlight` の各エントリを走査し、`seq + payloadLength <= ackNumber` なら削除。
    2. `lastAckedSeq` を `ackNumber` に更新。
- **呼び出し元**: ACK パケット受信時 (`BrowserBlockEntity.handleEstablishedAckSegment()`, `ServerBlockEntity.handleSendPhase()` 等)。

### `List<DataPacket> getUnacknowledgedPackets()`
- **概要**: `inFlight` に残っている ACK 未受信のパケットをリストとして返します。
- **呼び出し元**: デバッグや再送制御でウィンドウの状態を確認したいとき。

### `List<DataPacket> getTimedOutPackets()`
- **概要**: タイムアウト時間 (`DEFAULT_TIMEOUT_MILLIS`) を過ぎても ACK が返ってこないパケットを検出し、そのリストを返します。また該当パケットの送信時刻を現在時刻に更新します。
- **処理内容**:
    1. `now = System.currentTimeMillis()` で現在時刻を取得。
    2. `inFlight` を走査し、`now - lastSentTime > DEFAULT_TIMEOUT_MILLIS` ならタイムアウトとみなしてリストに追加。
    3. タイムアウトパケットのタイムスタンプを `refreshTimestamp()` で更新。
- **戻り値**: タイムアウトしたパケットのリスト。
- **呼び出し元**: サーバ側やクライアント側の `tickServer()` で再送制御 (`tickServer()` 内のタイムアウト処理)。

### `int getLastAckedSeq()`
- **概要**: 最後に ACK を受信したシーケンス番号を返します。
- **呼び出し元**: デバッグや送信ロジックの判断材料 (`handleEstablishedAckSegment()`, `handleSendPhase()` 等)。

### `int getWindowSize()`
- **概要**: 現在設定されているウィンドウサイズを返します。

---

## 呼び出し関係図（関数依存）
```
SlidingWindow(initialSeq, windowSize)
├─ initializes nextSeqNumber, windowSize, lastAckedSeq

queueData(packet)
└─ sendQueue.offer(packet)

getPacketsToSend()
├─ while (sendQueue not empty & inFlight size < windowSize)
│    ├─ sendQueue.poll()
│    ├─ Message(packet.getData())
│    ├─ Body.extractBody()
│    ├─ inFlight.put(nextSeqNumber, new TimedPacket())
│    └─ nextSeqNumber += payloadLength
└─ return ready list

acknowledge(ackNumber)
├─ iterate inFlight
│    ├─ Message(entry.packet.getData())
│    ├─ Body.extractBody()
│    └─ if (seq + payloadLength <= ackNumber) remove
└─ update lastAckedSeq

getUnacknowledgedPackets()
└─ return list of inFlight.packet

getTimedOutPackets()
├─ now = current time
├─ iterate inFlight
│    ├─ if timed out, add to list
│    └─ refreshTimestamp()
└─ return timedOut list

getLastAckedSeq(), getWindowSize()
└─ return respective field
```

---

## 外部依存関係
| クラス／ライブラリ | 用途 |
|------------------|----|
| **`DataPacket`** | 実際に送信するデータパケットを保持。送信キューや未確認パケットに格納される。 |
| **`Message`**, **`Body`** | パケットのデータ部分からペイロード長を取得するために使用。ペイロードの長さをシーケンス番号の増加分として利用。 |
| **`java.util.Queue`, `LinkedList`, `Map`, `HashMap`, `List`, `ArrayList`, `Iterator`, `PriorityQueue`** | キューやマップ、リストを用いて送信待ちや送信中、タイムアウト検出を管理。 |
| **`java.nio.charset.StandardCharsets`** | ペイロードをバイト列に変換する際に使用し、UTF-8 文字列のバイト長を取得。 |
| **`System.currentTimeMillis()`** | タイムアウト判定および送信時刻更新に使用。 |

---

## ファイル別概要
| ファイル名／クラス名 | 主な責務 | 主なメソッド |
|--------------------|-----------|-------------|
| **`SlidingWindow.java`** | スライディングウィンドウ方式によるデータ送信管理を行い、送信キューからの取り出し、ACK 処理、タイムアウト再送制御を提供する。 | コンストラクタ、`queueData()`, `getPacketsToSend()`, `acknowledge()`, `getUnacknowledgedPackets()`, `getTimedOutPackets()`, `getLastAckedSeq()`, `getWindowSize()` |
