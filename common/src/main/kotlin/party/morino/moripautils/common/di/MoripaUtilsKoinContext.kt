/*
 * Written in 2026 by Nikomaru <nikomaru@nikomaru.dev>
 *
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide.This software is distributed without any warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package party.morino.moripautils.common.di

import org.koin.core.Koin
import org.koin.core.KoinApplication
import org.koin.core.module.Module
import org.koin.dsl.koinApplication

/**
 * MoripaUtils 専用の Koin コンテナを保持するホルダー
 *
 * MineAuth など同じサーバー上の他プラグインも Koin を使うため、共有される GlobalContext (startKoin) は使わず、
 * このプラグイン専用の [KoinApplication] を生成して保持する。
 * Paper / Velocity の JAR はそれぞれ common を同梱し、さらに org.koin を relocate するため、
 * このオブジェクトはプラグインごとに独立したものになる。
 */
object MoripaUtilsKoinContext {
    /** 起動中の Koin アプリケーション (未起動または停止後は null) */
    @Volatile
    private var koinApplication: KoinApplication? = null

    /**
     * 専用の Koin コンテナを起動する
     *
     * 既に起動している場合 (前回の無効化に失敗した場合など) は、古いコンテナを閉じてから作り直す。
     *
     * @param modules 読み込むモジュールの一覧
     * @return 起動した Koin アプリケーション
     */
    @Synchronized
    fun start(modules: List<Module>): KoinApplication {
        // 古いインスタンスが残っていると定義が混ざるため、必ず作り直す
        koinApplication?.close()
        val application = koinApplication {
            modules(modules)
        }
        koinApplication = application
        return application
    }

    /**
     * 起動中のコンテナにモジュールを追加する
     *
     * 機能 (ticket など) ごとのモジュールを、設定で有効な場合だけ後から読み込むときに使う。
     *
     * @param modules 追加するモジュールの一覧
     * @throws IllegalStateException コンテナが起動していない場合
     */
    fun loadModules(modules: List<Module>) {
        get().loadModules(modules)
    }

    /**
     * 起動中の Koin を取得する
     *
     * @return 起動中の Koin
     * @throws IllegalStateException コンテナが起動していない場合
     */
    fun get(): Koin = checkNotNull(koinApplication) { "MoripaUtils Koin container is not started" }.koin

    /**
     * 起動中の Koin を取得する (起動していなければ null)
     *
     * 有効化に失敗した後の無効化処理など、起動しているか分からない場面で使う。
     *
     * @return 起動中の Koin、または null
     */
    fun getOrNull(): Koin? = koinApplication?.koin

    /**
     * 専用の Koin コンテナを停止する
     *
     * 起動していない場合は何もしない。
     */
    @Synchronized
    fun stop() {
        koinApplication?.close()
        koinApplication = null
    }
}
