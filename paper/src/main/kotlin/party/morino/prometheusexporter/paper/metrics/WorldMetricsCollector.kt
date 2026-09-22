/*
 * Written in 2026 by Nikomaru <nikomaru@nikomaru.dev>
 *
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide.This software is distributed without any warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package party.morino.prometheusexporter.paper.metrics

import io.prometheus.metrics.core.metrics.Gauge
import io.prometheus.metrics.model.registry.PrometheusRegistry
import org.bukkit.Server
import org.bukkit.World
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import party.morino.prometheusexporter.common.metrics.SampledMetricsCollector

/**
 * ワールドごとのエンティティ数 / チャンク数 / 時刻 / 天候などを公開するコレクター
 *
 * ラベル world にはワールドのキー (例: minecraft:overworld) を使う。
 * アンロードされたワールドのデータポイントは次のサンプリング時に削除する。
 */
class WorldMetricsCollector :
    SampledMetricsCollector,
    KoinComponent {
    private val server: Server by inject()

    override val id: String = "world"

    /** ワールド内のエンティティ数 */
    private val entities: Gauge = buildWorldGauge("minecraft_world_entities", "Number of entities in the world")

    /** ロード済みチャンク数 */
    private val chunksLoaded: Gauge = buildWorldGauge("minecraft_world_chunks_loaded", "Number of loaded chunks in the world")

    /** ワールド内のプレイヤー数 */
    private val players: Gauge = buildWorldGauge("minecraft_world_players", "Number of players in the world")

    /** ブロックエンティティ (チェストやかまどなど) の数 */
    private val blockEntities: Gauge = buildWorldGauge("minecraft_world_block_entities", "Number of block entities in the world")

    /** 毎 tick 処理されるブロックエンティティの数 */
    private val tickableBlockEntities: Gauge = buildWorldGauge(
        "minecraft_world_tickable_block_entities",
        "Number of tickable block entities in the world",
    )

    /** ワールド内の時刻 (0-24000 tick) */
    private val timeTicks: Gauge = buildWorldGauge("minecraft_world_time_ticks", "Time of day in the world in ticks (0-24000)")

    /** ワールド作成からの経過 tick 数 */
    private val gameTimeTicks: Gauge = buildWorldGauge("minecraft_world_game_time_ticks", "Total game time of the world in ticks")

    /** 雨 / 雪が降っているかどうか (1 = 降っている) */
    private val storm: Gauge = buildWorldGauge("minecraft_world_storm", "Whether it is raining or snowing in the world (1) or not (0)")

    /** 雷雨かどうか (1 = 雷雨) */
    private val thundering: Gauge = buildWorldGauge("minecraft_world_thundering", "Whether it is thundering in the world (1) or not (0)")

    /** アンロード検知のためにまとめて扱う Gauge の一覧 */
    private val worldGauges: List<Gauge> = listOf(
        entities,
        chunksLoaded,
        players,
        blockEntities,
        tickableBlockEntities,
        timeTicks,
        gameTimeTicks,
        storm,
        thundering,
    )

    /** 前回のサンプリングで存在したワールドのキー */
    private var previousWorldKeys: Set<String> = emptySet()

    override fun register(registry: PrometheusRegistry) {
        worldGauges.forEach { registry.register(it) }
    }

    override fun sample() {
        val worlds = server.worlds
        val currentWorldKeys = worlds.map { it.worldKey() }.toSet()

        // アンロードされたワールドのデータポイントを削除して、古い値が残り続けないようにする
        (previousWorldKeys - currentWorldKeys).forEach { key ->
            worldGauges.forEach { it.remove(key) }
        }
        previousWorldKeys = currentWorldKeys

        worlds.forEach { world -> sampleWorld(world) }
    }

    /**
     * 1 つのワールドの値を各 Gauge に書き込む
     *
     * @param world 対象のワールド
     */
    private fun sampleWorld(world: World) {
        val key = world.worldKey()
        entities.labelValues(key).set(world.entityCount.toDouble())
        chunksLoaded.labelValues(key).set(world.chunkCount.toDouble())
        players.labelValues(key).set(world.playerCount.toDouble())
        timeTicks.labelValues(key).set(world.time.toDouble())
        gameTimeTicks.labelValues(key).set(world.gameTime.toDouble())
        storm.labelValues(key).set(world.hasStorm().toGaugeValue())
        thundering.labelValues(key).set(world.isThundering.toGaugeValue())
        // ブロックエンティティ系は実装環境によって未対応のことがあるため最後に読む (途中で失敗しても他の値は残る)
        blockEntities.labelValues(key).set(world.tileEntityCount.toDouble())
        tickableBlockEntities.labelValues(key).set(world.tickableTileEntityCount.toDouble())
    }

    /** ワールドを識別するラベル値 (getName は非推奨のためキーを使う) */
    private fun World.worldKey(): String = key.asString()

    /** Boolean を Gauge 用の 0 / 1 に変換する */
    private fun Boolean.toGaugeValue(): Double = if (this) 1.0 else 0.0

    /**
     * world ラベル付きの Gauge を生成する
     *
     * @param name メトリクス名
     * @param help ヘルプテキスト
     */
    private fun buildWorldGauge(name: String, help: String): Gauge = Gauge.builder()
        .name(name)
        .help(help)
        .labelNames(WORLD_LABEL)
        .build()

    companion object {
        /** ワールドを区別するラベル名 */
        private const val WORLD_LABEL = "world"
    }
}
