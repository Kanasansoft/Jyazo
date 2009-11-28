Jyazo is licensed under the GPL.

Sorry. This file is only written in Japanese.


========================================
Jyazo
----------------------------------------

JyazoはJavaで実装されたGyazoのクローンです。
Windows, Mac OS X, Linux問わず実行できるはずです。


========================================
そもそもGyazoとは
----------------------------------------

画面のキャプチャ画像を簡単に公開・共有するサービスです。
詳細な情報は以下のURLを参照ください。
http://gyazo.com/


========================================
実行ファイル
----------------------------------------

各OS向けの実行ファイルを同梱しています。
「x.x.x」はバージョンを示します。

	Windows用実行ファイル
		Jyazo-x.x.x.exe
	Mac OS X用実行ファイル
		Jyazo-x.x.x.dmg内のJyazo.app
	Linux用実行ファイル
		Jyazo-x.x.x.jar

Linuxの場合、以下のようなシェルスクリプトを
jarファイルと同じディレクトリに保存し実行権限を追加しておくと
こちらのファイルから起動できるはずです。
(バージョン番号は適時書き変えて下さい。)

#! /bin/sh

shell_script_path=${PWD}"/"${BASH_SOURCE[0]}
jar_file_path=`expr ${shell_script_path} : '\(.*/\)[^\/]*$'`Jyazo-x.x.x.jar
java -jar ${jar_file_path}


========================================
Javaランタイム
----------------------------------------

JRE6(Java1.6)以上が必須環境となります。


========================================
Proxyサーバ対応
----------------------------------------

Proxyサーバに対応しています。
但し、Proxy認証には対応していません。
Proxyサーバの設定方法は、「設定ファイルの記述方法」を参照して下さい。


========================================
クロスポスト対応
----------------------------------------

キャプチャした画像を複数のGyazoサーバに同時に送信可能です。
また、クロスポストの送信先を簡単に切り換える事が可能です。
クロスポストの設定方法は、「設定ファイルの記述方法」を参照して下さい。


========================================
設定ファイル
----------------------------------------

初回のJyazoの実行時に、ユーザのホームに
「.jyazo」 ディレクトリ(フォルダ)が作成されます。
「.jyazo」ディレクトリ配下にある「setting.properties」が設定ファイルになります。
同ディレクトリに設定ファイルのサンプル「setting-sample.properties」が
ありますので、クロスポスト機能の設定がそちらを参考にして下さい。
「.jyazo」ディレクトリを削除し、再度Jyazoを実行すると、
「.jyazo」ディレクトリが再生成されます。


========================================
ローカルGyazoサーバの構築方法
----------------------------------------

以下のURLに記事を書きました。
http://www.kanasansoft.com/weblab/2009/11/local_gyazo_server.html
ローカルGyazoサーバの構築方法はこれだけではありませんが、
参考になれば幸いです。


========================================
キャプチャ機能について
----------------------------------------

現バージョンではマルチディスプレイ環境には対応していません。
キャプチャの対象はプライマリモニタだけになります。
GyazoではOS標準のキャプチャ機能を使用していますが、Jyazoではキャプチャ機能も作り込んでいます。
Jyazoを実行した直後に取得したキャプチャ画像を使い回しています。
これは、画面をキャプチャしながら選択範囲をリアルタイムで描画すると処理が遅すぎるためです。


========================================
IDについて
----------------------------------------

Gyazoで使用しているユーザ(もしくは端末)を識別するために使用しているIDは、
時間(最小単位が秒)を元に算出しているため、
Jyazoではナノ秒まで取得しIDを作成しています。
但し、時間解像度はそこまで高くないため、末尾には0が並びます。
また、IDの最後に「_by_Jyazo」を付加しています。


========================================
開発者の方へ
----------------------------------------

ソースファイルは、GitHubに公開してます。
http://github.com/Kanasansoft/Jyazo

ライセンスは、Gyazoと同様にGPLを採用しています。

JyazoはJava初心者が作成しています。
コード内に稚拙な部分があるかと思います。
改善点がありましたらどうぞご指摘ください。

Apache Mavenのバージョン2系を利用しています。
pom.xmlも同梱していますので、
利用しているプラグイン等が一度に取得可能です。

Jyazoの実装はJava6の標準ライブラリのみで行なっており、
サードパーティ製のライブラリは使用していません。
ただし、コンパイルやパッケージングは、
Maven2を通してサードパーティ製のライブラリを
利用しています。


========================================
設定ファイルの記述方法
----------------------------------------

初回実行直後の設定ファイルは、
Gyazoと同じ動作になるように記述されています。
設定ファイルを書き変える事により
Proxyサーバ対応、クロスポストが可能となります。

設定ファイルの記述方法は、
JavaのPropertiesファイルと同様です。

一度のJyazo実行で複数のGyazoサーバにクロスポストする設定のことを
JyazoではPostSetと読んでいます。
PostSetを複数準備すると、キーボードからPostSetを選択できるようになります。
例えば、自端末にGyazoサーバをlocalhostとし、Gyazoの公式サーバをpublicとします。

	一番目のPostSet
		publicにのみ画像をポスト
	二番目のPostSet
		localhostのみに画像をポスト
	三番目のPostSet
		publicとlocalhostに画像をクロスポスト

設定ファイルには、サーバの設定、PostSetの設定、
更に使用するサーバをJyazoに通知するための設定と
使用するPostSetをJyazoに通知するための設定があります。
また、Jyazo起動時に、PostSetを判別し易いように、
見た目を変更する設定もあります。

以下、設定ファイルを記述していきます。
完成版はユーザのホーム配下の「~/.jyazo/setting-sample.properties」に
出力されています。

まず、サーバの設定を行ないます。

	post_sets.server.[サーバID].url=[GyazoサーバのURL]
	post_sets.server.[サーバID].use_proxy=[Proxy使用の有無]
	post_sets.server.[サーバID].proxy_host=[ProxyサーバのURL]
	post_sets.server.[サーバID].proxy_port=[ProxyサーバのPort]

公式のGyazoサーバのIDをpublicとすると以下のような記述になります。
Proxyサーバ「192.168.0.100:8080」を使用することを想定しています。
(Propertiesファイルでは「:」のエスケープが必要な点に注意してください。)

	post_sets.server.public.url=http\://gyazo.com/upload.cgi
	post_sets.server.public.use_proxy=yes
	post_sets.server.public.proxy_host=192.168.0.100
	post_sets.server.public.proxy_port=8080

同様に、時端末に構築したローカルGyazoサーバの設定をします。
ローカルGyazoサーバのIDは「localhost」とします。
自端末のため、Proxyサーバは使用しません。

	post_sets.server.localhost.url=http\://localhost/cgi-bin/gyazo/upload.cgi
	post_sets.server.localhost.use_proxy=no
	post_sets.server.localhost.proxy_host=192.168.0.100
	post_sets.server.localhost.proxy_port=8080

これでふたつのサーバ、publicとlocalhostの設定が終わりました。

それでは、PostSetを3つ作成していきます。

	post_sets.post_set.[PostSetID].name=[PostSet名(キャプチャ時に画面に表示)]
	post_sets.post_set.[PostSetID].text_size=[PostSet名表示の文字サイズ]
	post_sets.post_set.[PostSetID].text_color=[PostSet名表示の文字色]
	post_sets.post_set.[PostSetID].select_area_color=[キャプチャ時の選択範囲矩形の色]
	post_sets.post_set.[PostSetID].unselect_area_color=[キャプチャ時の選択範囲外の色]
	post_sets.post_set.[PostSetID].server_ids=[ポストするサーバID(スペース区切りで複数指定可能)]

まず、公式GyazoサーバのみにポストするPostSetを設定します。
PostSetのIDをpublic_onlyとしました。
色の指定は、「アルファ値・赤・緑・青」各16進法2桁の計8文字となります。
キャプチャ中にデスクトップが見にくくなるため、アルファ値は低めに設定して下さい。

	post_sets.post_set.public_only.name=Public Only
	post_sets.post_set.public_only.text_size=96
	post_sets.post_set.public_only.text_color=33ff0000
	post_sets.post_set.public_only.select_area_color=33ff6666
	post_sets.post_set.public_only.unselect_area_color=33ff9999
	post_sets.post_set.public_only.server_ids=public

同様に、ローカルGyazoサーバのみにポストするPostSetの設定です。
IDはlocalhost_onlyとしました。
ここでは、文字サイズと各種色指定を省略しています。
省略された場合、既定として設定された値(後述)が使用されます。

	post_sets.post_set.localhost_only.name=Localhost Only
	post_sets.post_set.localhost_only.server_ids=localhost

次に、公式とローカルのGyazoサーバにクロスポストするPostSetの設定です。
PostSetのIDはpublic_and_localhostです。
複数のサーバにポストするために、server_idsにサーバIDをスペース区切りで複数指定しています。

	post_sets.post_set.public_and_localhost.name=Public and Localhost
	post_sets.post_set.public_and_localhost.text_size=96
	post_sets.post_set.public_and_localhost.text_color=330000ff
	post_sets.post_set.public_and_localhost.select_area_color=336666ff
	post_sets.post_set.public_and_localhost.unselect_area_color=339999ff
	post_sets.post_set.public_and_localhost.server_ids=public localhost

ここまでで、サーバとPostSetの設定が完了しました。
使用するサーバとポストセットをJyazoに通知するために以下の行を追加します。

	post_sets.post_set_ids=[使用するするPostSetID(スペース区切りで複数指定可能)]
	post_sets.server_ids=[使用するサーバID(スペース区切りで複数指定可能)]

ここまでの設定を通知する場合は次のようになります。

	post_sets.post_set_ids=public_only localhost_only public_and_localhost
	post_sets.server_ids=public localhost

ここまででクロスポストの設定は完了です。
PostSetを切り換えるには、
Jyazo起動後の範囲選択待ちの画面でキーボードの数字キーを使用します。
Jyazoに通知した順に、「1」キーから割り当てられていきます。
PostSetは最大10つ設定でき、10番目のPostSetは「0」キーに割り当てられます。

最後に、PostSetで文字サイズや色を指定しなかった場合に使用される既定の値を設定します。

	post_sets.text_size=[PostSet名表示の文字サイズ]
	post_sets.text_color=[PostSet名表示の文字色]
	post_sets.select_area_color=[キャプチャ時の選択範囲矩形の色]
	post_sets.unselect_area_color=[キャプチャ時の選択範囲外の色]

ここでは以下の用に設定しました。

	post_sets.text_size=96
	post_sets.text_color=3300ff00
	post_sets.select_area_color=3366ff66
	post_sets.unselect_area_color=3399ff99

以上が設定ファイルの記述方法となります。
ここで作成した設定ファイルと同一の設定をもつサンプルファイルが、
Jyazoの初回起動時に
「~/.jyazo/setting-sample.properties」に出力されています。
