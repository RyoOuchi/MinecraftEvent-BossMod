# コードドキュメント

## 概要
`ConnectionState` クラスは、クライアントとサーバ間の仮想 TCP 通信におけるシーケンス番号の状態を保持するための不変データ構造です。Java の record 構文を使用しており、`clientSeq` と `serverSeq` という2つの整数フィールドを持ちます。主な役割は、双方のシーケンス番号を管理し、それぞれの ACK 番号の妥当性を検証できるようにすることです。

- **クライアント側 SEQ (`clientSeq`)**: クライアントが次に送信すべきシーケンス番号。
- **サーバ側 SEQ (`serverSeq`)**: サーバが次に送信すべきシーケンス番号。

このクラスは不変であり、状態更新は新しい `ConnectionState` インスタンスを返すメソッドを通じて行われます。スレッドセーフな設計で、TCP ハンドシェイクやデータ送受信ロジックの信頼性を支えています。

---

## グローバル変数一覧
`ConnectionState` はレコードであるため、クラスレベルのグローバル変数は存在しません。インスタンス生成時に次の2つのフィールドが固定されます。

| フィールド名 | 型 | 初期値 | 使用箇所 | 説明 |
|--------------|----|--------|----------|------|
| `clientSeq` | `int` | コンストラクタ引数 | `updateServerSeq()`, `validateClientAckNumber()`, `getClientSeq()` | クライアントが次に送るべき SEQ 番号。 |
| `serverSeq` | `int` | コンストラクタ引数 | `updateClientSeq()`, `validateServerAckNumber()`, `getServerSeq()` | サーバが次に送るべき SEQ 番号。 |

---

## 関数一覧

### レコードコンストラクタ
- **概要**: `clientSeq` と `serverSeq` の2つの整数引数を受け取り、それぞれのフィールドを初期化します。
- **使用法**: `new ConnectionState(int clientSeq, int serverSeq)`。
- **呼び出し元**: TCP ハンドシェイク開始時や状態更新時に新しい状態を生成するとき。

### `ConnectionState updateClientSeq(int newClientSyn)`
- **概要**: クライアント側の SEQ 番号を更新した新しい `ConnectionState` を返します。
- **引数**: `newClientSyn` – 更新後のクライアント SEQ 番号。
- **戻り値**: `new ConnectionState(newClientSyn, this.serverSeq)` – サーバ SEQ は現在の値を引き継ぎ、クライアント SEQ を更新した新インスタンス。
- **呼び出し元**: クライアントから ACK を受信した後など、クライアントのシーケンスが進んだとき。

### `ConnectionState updateServerSeq(int newServerSyn)`
- **概要**: サーバ側の SEQ 番号を更新した新しい `ConnectionState` を返します。
- **引数**: `newServerSyn` – 更新後のサーバ SEQ 番号。
- **戻り値**: `new ConnectionState(this.clientSeq, newServerSyn)` – クライアント SEQ は現在の値を引き継ぎ、サーバ SEQ を更新した新インスタンス。
- **呼び出し元**: サーバが新たなシーケンス番号を決定したとき。

### `boolean validateClientAckNumber(int ackNumber)`
- **概要**: クライアントから送られてきた ACK 番号が `clientSeq + 1` と一致するか検証します。
- **引数**: `ackNumber` – 検証対象の ACK 番号。
- **戻り値**: `true` なら有効、`false` なら無効。
- **呼び出し元**: TCP ハンドシェイクやデータ受信時にクライアントの ACK を検証するロジック (`BrowserBlockEntity.receiveHandshakeServerResponse()` など)。

### `boolean validateServerAckNumber(int ackNumber)`
- **概要**: サーバから送られてきた ACK 番号が `serverSeq + 1` と一致するか検証します。
- **引数**: `ackNumber` – 検証対象の ACK 番号。
- **戻り値**: `true` なら有効、`false` なら無効。
- **呼び出し元**: サーバがクライアントに返す ACK を検証するロジック (`ServerBlockEntity.handleHandshake()` など)。

### `int getClientSeq()`, `int getServerSeq()`
- **概要**: 現在保持しているクライアント/サーバ側の SEQ 番号を返します。
- **戻り値**: それぞれ `clientSeq` または `serverSeq` の値。

---

## 呼び出し関係図（関数依存）
```
ConnectionState(clientSeq, serverSeq)
└─ initialises fields

updateClientSeq()
└─ new ConnectionState(newClientSeq, serverSeq)

updateServerSeq()
└─ new ConnectionState(clientSeq, newServerSeq)

validateClientAckNumber()
└─ compares ackNumber to clientSeq + 1

validateServerAckNumber()
└─ compares ackNumber to serverSeq + 1

getClientSeq(), getServerSeq()
└─ return respective field
```
---

## 外部依存関係
このクラスは純粋なデータ保持のレコードであり、外部ライブラリには依存しません。ただし、通信プロトコルの実装において以下のクラスから参照されます。

| クラス名 | 用途 |
|--------|------|
| **`BrowserBlockEntity`** | ハンドシェイクの応答処理で `validateClientAckNumber()` を使用し、`updateServerSeq()` でサーバ SEQ を更新する。 |
| **`ServerBlockEntity`** | ハンドシェイクフェーズやデータ送受信フェーズで `validateServerAckNumber()` を使用し、`updateClientSeq()` でクライアント SEQ を更新する。 |
| **`SlidingWindow`** | ウィンドウ管理の初期化に `getClientSeq()` と `getServerSeq()` を使用する。 |

---

## ファイル別概要
| ファイル名／クラス名 | 主な責務 | 主なメソッド |
|--------------------|-----------|-------------|
| **`ConnectionState.java`** | クライアントとサーバ間の SEQ 状態を保持する不変データ構造。ACK 番号の検証や SEQ 更新を提供する。 | コンストラクタ、`updateClientSeq()`, `updateServerSeq()`, `validateClientAckNumber()`, `validateServerAckNumber()`, `getClientSeq()`, `getServerSeq()` |
