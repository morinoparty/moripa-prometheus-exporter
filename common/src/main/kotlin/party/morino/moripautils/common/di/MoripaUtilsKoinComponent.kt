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
import org.koin.core.component.KoinComponent

/**
 * MoripaUtils 専用の Koin コンテナから依存を解決する [KoinComponent]
 *
 * 通常の [KoinComponent] は GlobalContext を参照するため、他プラグインのコンテナと衝突しうる。
 * このプロジェクトのクラスは [KoinComponent] の代わりにこのインターフェースを実装し、
 * これまで通り `by inject()` / `get()` で依存を取得する。
 */
interface MoripaUtilsKoinComponent : KoinComponent {
    /**
     * [MoripaUtilsKoinContext] が保持する専用コンテナを返す
     *
     * @return このプラグイン専用の Koin
     * @throws IllegalStateException コンテナが起動していない場合
     */
    override fun getKoin(): Koin = MoripaUtilsKoinContext.get()
}
