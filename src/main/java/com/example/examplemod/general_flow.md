## プロジェクト全体の流れ（ナラティブ形式）

このプロジェクトは、Minecraft の世界の中でインターネットや UNIX 風のシステムを模倣する総合的な Mod です。プレイヤーはルーターやケーブル、DNS サーバー、Web サーバーをブロックとして設置し、仮想ネットワークを構築します。さらにブラウザやターミナル画面も用意されており、ドメイン名でウェブページを閲覧したり、サーバーにログインしてコマンドを実行することができます。以下では、ゲーム内での一連の処理の流れを物語のように説明します。

## Mod の初期化とブロックの登録

ゲームが起動して本 Mod が読み込まれると、ExampleMod クラスが呼び出されます。このクラスでは WifiRouterBlock や CableBlock、DNSServerBlock、ServerBlock、BrowserBlock、VSCodeBlock といった主要なブロックを生成し、Forge のレジストリに登録します。ブロックに対応する BlockEntity もここで定義され、後述のネットワーク処理やデータ保存に利用されます。また、サーバ・クライアント間で通信するための SimpleChannel を初期化し、URL リクエスト、ターミナルコマンド、ブラウザ応答などの専用パケットを登録します。これによりゲーム内のイベントからネットワーク層へデータを送受信できるようになります。

## ネットワークの構築 – ルーターとケーブル

プレイヤーが世界に WifiRouterBlock を設置すると、その位置に WifiRouterBlockEntity が生成されます。このエンティティは起動時に connectToRouterNetwork() を呼び出し、周囲のケーブルブロックを走査して他のルーターと接続します。探索は幅優先探索 (BFS) で行われ、ケーブルの長さをコストとした隣接リストを Graph クラスに構築します。こうして出来上がった routerMap にはネットワーク内のすべてのルーター同士の距離が保存され、後で最短経路探索に利用されます。ルーターの上に DNSServerBlock や ServerBlock がある場合、それぞれ DNS サーバーやウェブサーバーとして機能します。その位置情報は DnsServerSavedData や ServerSavedData に保存され、ワールド間で永続化されます。

CableBlock は六方向に接続可能なケーブルを表し、隣接するケーブルやルーターを検出して自身の状態を更新します。ケーブルの中身を表す CableBlockEntity にはパケットキューがあり、ルーターから送られてきた DataPacket を順番に転送します。ケーブルはただの中継なので、受け取ったパケットを次のケーブルへ渡すか、行き止まりになった場合は隣接するルーターへ渡します。

## DNS サーバーの仕組み

DNSServerBlock は設置された瞬間にワールドの DnsServerSavedData に登録されます。DNSServerBlockEntity は DNS リクエストを受け取ると、まず ServerSavedData からドメイン名と IP（= サーバーブロックの座標）の対応表を読み込みます。パケットのデータとして渡される URL からドメイン名を抽出し、存在しない場合は ErrorCodes.NXDOMAIN を返します。存在する場合は BlockPos x:y:z 形式の文字列に変換して応答パケットを作成します。応答パケットでは routerPath を逆順にし、送信元と送信先ブロック座標を入れ替えて返送します。DNS 応答が準備されると自分の下のルーターへ渡され、ケーブル経由でクライアントへ戻ります。

## ブラウザブロックと DNS リクエスト

プレイヤーが BrowserBlock を右クリックすると、クライアント側では BrowserSearchScreen が開きます。URL を入力して「Go」を押すと RequestUrlPacket がサーバーへ送信されます。サーバー側ではこのパケットを受け取り、ブラウザブロックの周囲 5 ブロック以内で最も近いルーター (WifiRouterBlockEntity) を検索します。見つかったルーターに対して performDNSRequest() を呼び出し、URL をバイト列に変換した DataPacket を作成して DNS サーバーへ送る準備をします。この時点でブラウザブロック (BrowserBlockEntity) には送信予定の URL データが保存され、後のデータ送信で利用されます。

ルーターは routerMap から DNS サーバーが接続されたルーターまでの最短経路を求め、それを routerPath として DataPacket に格納します。さらに、現在のルーターと次のルーターとの間のケーブル経路を探索し、cablePath に格納します。パケットはケーブルのキューに追加され、CableBlockEntity の tickServer() で 1 ホップずつ次のケーブルへ転送されます。パケットが DNS サーバー下のルーターに到達すると、DNSServerBlockEntity.receiveDataPacket() が呼ばれ、先ほど述べたようにドメイン解決が行われます。

DNS 応答が戻ってくると、ブラウザブロックは receiveDNSResponse() によりそれを受信します。エラーコードが NXDOMAIN や FORMERR の場合はクライアントにエラーメッセージを送りますが、NOERROR の場合は応答に含まれる IP（サーバーブロックの座標）を serverIPAddress として保存します。続いて、ブラウザはランダムに生成した SEQ 番号を用いてサーバーとの TCP ハンドシェイクを開始します。

## TCP ハンドシェイクとデータ送信

ブラウザはまず SYN パケットに相当するメッセージを作り、Queries.TCP_HANDSHAKE としてルーターに送信します。このパケットにはクライアントの SEQ 番号だけが含まれます。ルーターは DNS のときと同様に routerMap からサーバーまでの経路を探してパケットを配送します。サーバー側 (ServerBlockEntity) では receiveDataPacket() が呼ばれ、Queries.TCP_HANDSHAKE として処理します。サーバーはクライアントの SEQ 番号を ConnectionState に保存し、自身の SYN 番号を生成して SYN+ACK を返します。

ブラウザがサーバーからの SYN+ACK を receiveHandshakeServerResponse() で受け取ると、ACK 番号を検証し、正しい場合は ACK を送り返して接続が確立したことを相手に伝えます。その後 SlidingWindow を初期化します。スライディングウィンドウはウィンドウサイズ 3、タイムアウト 3 秒で設定されており、最大 3 個の未確認パケットを同時に送ることができます。

URL データの送信では、ブラウザブロックは保存しておいた URL バイト列を 8 バイトずつに分割し、それぞれの塊を TCP パケット形式に変換します。ヘッダーには SEQ 番号、ACK 番号、最後のフラグ (END=1 ならこの塊が最後) が含まれます。ブラウザは sendPacketWithSlidingWindow() により、送信可能な枠が空いている分だけパケットをルーターに渡します。ルーターはケーブル経由でサーバーに届け、サーバーでは Queries.TCP_ESTABLISHED として受信・確認応答を返します。ブラウザは ACK を受け取るとその SEQ 番号までのデータを SlidingWindow から除き、新たなパケットを送るスペースを確保します。パケットロスを模擬するため、ブラウザとサーバー双方で 20% の確率で受信データを無視するようになっており、同じ ACK を複数回受け取った場合は高速再送 (fastRetransmit()) が発動します。

## サーバー側のデータ処理

サーバーブロックはハンドシェイクが完了すると、ブラウザが送ってきた URL の後半部分（パス）を読み取り、内部の仮想ファイルシステムを参照します。ファイルシステムは FileSystem クラスで実装されており、ディレクトリ (DirectoryNode) とファイル (FileNode) をツリー状に保持します。ServerBlockEntity には CommandInterpreter が組み込まれており、ls、cd、mkdir、touch、rm、upload、cat、echo、help などの UNIX 風コマンドを解釈・実行できます。ターミナル画面 (TerminalScreen) から送られる TerminalCommandPacket はこのインタプリタに渡され、標準出力は TerminalOutputPacket としてプレイヤーに返されます。

Web サーバーとしての振る舞いでは、URL のパスをファイルパスとして解釈し、そのファイルの内容を読み取ってクライアントへ返します。例えば /index.html を要求された場合、仮想ファイルシステムにそのファイルが存在すれば、その中身を 8 バイトずつ分割して TCP パケットとして送信します。存在しない場合はエラーメッセージを返します。送信側でも SlidingWindow を用いてパケットを管理し、タイムアウトが発生したパケットは再送されます。全てのデータを送った後は、END フラグ付きのパケットを送り、最後に Queries.TCP_DISCONNECT を送って接続終了を通知します。サーバーはこの通知を受けて各種状態 (clientSyncMap や slidingWindowMap) を破棄し、次の接続に備えます。

## ブラウザでのデータ受信と表示

ブラウザブロックはサーバーから受け取った TCP パケットを receiveEstablishedServerResponse() で処理します。パケットの RESPONSE 番号が 1 ならデータセグメントとみなし、expectedSeqNumber が一致する場合は順番通り受け取ったと判断して receivedDataChunks に保存し、expectedSeqNumber を更新します。expectedSeqNumber より大きい SEQ のパケットは先に届いてもバッファに保存し、欠損パケットの ACK を再送することで再送要求を行います。NetworkUtils.reconstructData() を使って連続するデータを結合し、END フラグが立ったパケットが到達し、なおかつ全てのデータが連続していることが確認できればファイル全体が復元されたことになります。

復元が完了すると、ブラウザはサーバーへ DISCONNECT パケットを送り、サーバーが接続状態を解放します。そして BrowserResponsePacket をクライアントへ送信し、クライアント側では BrowserDisplayScreen が開いて HTML コンテンツやメッセージを描画します。このようにして、プレイヤーはゲーム内で実際にウェブサイトを閲覧しているような体験を得ます。

## ドメイン登録と VSCode ブロック

ServerBlock の側面にはドメイン名を登録する仕組みも用意されています。プレイヤーが ServerBlock を右クリックすると ServerDomainScreen が表示され、そこにドメイン名を入力して AddServerPacket を送信できます。サーバー側では ServerSavedData.addServer() が呼ばれ、ドメインとサーバーブロックの座標が保存されます。これにより DNS サーバーはそのドメインを解決できるようになります。OnMinecraftInstanceLaunched イベントでは、ワールド読み込み時に保存されているサーバー情報をチェックし、存在しないサーバーブロックに対する登録を削除します。

さらに、VSCodeBlock は CodeFileItem を保管できるブロックで、GUI コンテナ (VSCodeBlockContainer) を通じてプレイヤーがコードファイルを挿入できます。CodeFileItem はクリックするとデバッグ用に HTML テストページを表示する仕組みを備えています。こうした要素により、単なるネットワークシミュレーションにとどまらず、ファイル編集やサーバー管理まで含めた総合的なシステムとなっています。

## 全体の流れのまとめ

このプロジェクト全体の流れを振り返ると、まず Mod の初期化でネットワーク要素が登録され、プレイヤーはルーターとケーブルを設置して物理的なネットワークを構築します。ブラウザブロックから URL を入力すると、ルーターは DNS サーバーに問い合わせてサーバーの座標を取得し、TCP ハンドシェイクを行った後、スライディングウィンドウ方式でデータを送受信します。サーバーブロックは仮想ファイルシステムにアクセスしてリクエストを処理し、結果をブラウザへ返します。通信完了後は双方が接続を切断し、ブラウザは結果を画面に表示します。この一連の手続きがゲーム内で視覚化され、プレイヤーはブロックを通じてネットワークの仕組みやプロトコルを学びながら楽しむことができます。