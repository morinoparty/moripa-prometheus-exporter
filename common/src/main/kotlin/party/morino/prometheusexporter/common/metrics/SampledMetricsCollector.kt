/*
 * Written in 2026 by Nikomaru <nikomaru@nikomaru.dev>
 *
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide.This software is distributed without any warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package party.morino.prometheusexporter.common.metrics

/**
 * 定期的にサンプリングして値を更新するコレクター
 *
 * Bukkit API のようにメインスレッドからしか安全に読めない値は、
 * スクレイプ時のコールバックではなく [sample] でメインスレッド上から Gauge に書き込む。
 */
interface SampledMetricsCollector : MetricsCollector {
    /**
     * 現在の値を読み取り、メトリクスへ反映する
     *
     * プラットフォームのメインスレッドから定期的に呼ばれるため、
     * 処理は軽量に保ち、ブロッキングする操作を行ってはならない。
     */
    fun sample()
}
