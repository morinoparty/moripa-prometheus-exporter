/*
 * Written in 2026 by Nikomaru <nikomaru@nikomaru.dev>
 *
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide.This software is distributed without any warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package party.morino.prometheusexporter.paper.metrics

import io.prometheus.metrics.core.metrics.Counter
import io.prometheus.metrics.model.registry.PrometheusRegistry
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent
import party.morino.prometheusexporter.common.metrics.MetricsCollector

/**
 * チャンクのロード / アンロード回数をカウントするリスナー
 *
 * これらのイベントは非常に高頻度で発火するため、ハンドラではワールドのキーと新規生成フラグ以外を読まない。
 */
class ChunkEventListener :
    MetricsCollector,
    Listener {
    override val id: String = "chunk_events"

    /** チャンクのロード回数 (new ラベルが true なら新規生成されたチャンク) */
    private val chunkLoads: Counter = Counter.builder()
        .name("minecraft_chunk_loads_total")
        .help("Total number of chunk loads by world (new=true when the chunk was newly generated)")
        .labelNames("world", "new")
        .build()

    /** チャンクのアンロード回数 */
    private val chunkUnloads: Counter = Counter.builder()
        .name("minecraft_chunk_unloads_total")
        .help("Total number of chunk unloads by world")
        .labelNames("world")
        .build()

    override fun register(registry: PrometheusRegistry) {
        registry.register(chunkLoads)
        registry.register(chunkUnloads)
    }

    /**
     * チャンクロード時にカウントする
     *
     * @param event ロードイベント
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onChunkLoad(event: ChunkLoadEvent) {
        chunkLoads.labelValues(event.world.key.asString(), event.isNewChunk.toString()).inc()
    }

    /**
     * チャンクアンロード時にカウントする
     *
     * @param event アンロードイベント
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onChunkUnload(event: ChunkUnloadEvent) {
        chunkUnloads.labelValues(event.world.key.asString()).inc()
    }
}
