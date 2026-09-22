/*
 * Written in 2026 by Nikomaru <nikomaru@nikomaru.dev>
 *
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide.This software is distributed without any warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package party.morino.prometheusexporter.velocity.metrics

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.connection.PostLoginEvent
import com.velocitypowered.api.event.player.KickedFromServerEvent
import com.velocitypowered.api.event.player.ServerConnectedEvent
import com.velocitypowered.api.event.proxy.ProxyPingEvent
import com.velocitypowered.api.network.ProtocolState
import io.prometheus.metrics.core.metrics.Counter
import io.prometheus.metrics.model.registry.PrometheusRegistry
import party.morino.prometheusexporter.common.metrics.MetricsCollector

/**
 * プレイヤーの接続 / 切断 / サーバー移動 / キック / サーバーリスト ping をカウントするイベントリスナー
 *
 * Velocity のリスナーは @Subscribe メソッドを持つ任意のオブジェクトなので、
 * プラグイン側で `eventManager.register(plugin, listener)` に渡して登録する。
 * ハンドラーはカウンターを進めるだけの軽量な処理なので、同期 (async = false) で実行する。
 */
class ConnectionEventListener : MetricsCollector {
    override val id: String = "connection_events"

    /** プロキシへのログイン完了回数 (他プラグインに拒否されたログイン試行は含まない) */
    private val logins: Counter = Counter.builder()
        .name("velocity_logins_total")
        .help("Total number of completed player logins to the proxy")
        .build()

    /** プロキシからの切断回数 (ログイン状態ごと) */
    private val disconnects: Counter = Counter.builder()
        .name("velocity_disconnects_total")
        .help("Total number of player disconnects from the proxy by login status")
        .labelNames("status")
        .build()

    /** バックエンドサーバーへの接続回数 (接続先と移動元ごと) */
    private val serverConnections: Counter = Counter.builder()
        .name("velocity_server_connections_total")
        .help("Total number of backend server connections by target and previous server")
        .labelNames("server", "previous_server")
        .build()

    /** バックエンドサーバーからキックされた回数 (サーバーとフェーズごと) */
    private val serverKicks: Counter = Counter.builder()
        .name("velocity_server_kicks_total")
        .help("Total number of kicks from backend servers by server and connection phase")
        .labelNames("server", "phase")
        .build()

    /** サーバーリスト ping の回数 */
    private val pings: Counter = Counter.builder()
        .name("velocity_pings_total")
        .help("Total number of server list pings received by the proxy")
        .build()

    override fun register(registry: PrometheusRegistry) {
        registry.register(logins)
        registry.register(disconnects)
        registry.register(serverConnections)
        registry.register(serverKicks)
        registry.register(pings)
    }

    /**
     * プレイヤーのプロキシへのログインが完了したときにカウントする
     *
     * LoginEvent は他プラグインが後から拒否する可能性があるため、拒否されなかったログインだけが届く
     * PostLoginEvent を使う。これにより Velocity 自身が弾く重複ログインなども数えない。
     */
    @Subscribe(async = false)
    @Suppress("UnusedParameter")
    fun onLogin(event: PostLoginEvent) {
        logins.inc()
    }

    /**
     * プレイヤーがプロキシから切断したときにログイン状態をラベルにしてカウントする
     */
    @Subscribe(async = false)
    fun onDisconnect(event: DisconnectEvent) {
        disconnects.labelValues(event.loginStatus.name.lowercase()).inc()
    }

    /**
     * プレイヤーがバックエンドサーバーに接続したときに接続先と移動元をラベルにしてカウントする
     */
    @Subscribe(async = false)
    fun onServerConnected(event: ServerConnectedEvent) {
        // 初回接続では移動元がないため "none" とする
        val previousServer = event.previousServer.map { it.serverInfo.name }.orElse("none")
        serverConnections.labelValues(event.server.serverInfo.name, previousServer).inc()
    }

    /**
     * プレイヤーがバックエンドサーバーからキックされたときにサーバーとフェーズをラベルにしてカウントする
     *
     * phase は接続処理中のキックなら "connect"、接続済みのプレイ中のキックなら "play" になる。
     * (Velocity API の kickedDuringLogin() は kickedDuringServerConnect() の非推奨エイリアスで同じ値を返すため、
     * "login" は区別できない)
     */
    @Subscribe(async = false)
    fun onKickedFromServer(event: KickedFromServerEvent) {
        val phase = if (event.kickedDuringServerConnect()) "connect" else "play"
        serverKicks.labelValues(event.server.serverInfo.name, phase).inc()
    }

    /**
     * サーバーリスト ping を受け取ったときにカウントする
     *
     * ログイン中の接続などに対しても ProxyPingEvent が発火しうるため、STATUS 状態の接続のみを数える。
     */
    @Subscribe(async = false)
    fun onProxyPing(event: ProxyPingEvent) {
        if (event.connection.protocolState == ProtocolState.STATUS) {
            pings.inc()
        }
    }
}
