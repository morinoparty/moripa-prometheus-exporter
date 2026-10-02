/*
 * Written in 2026 by Nikomaru <nikomaru@nikomaru.dev>
 *
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide.This software is distributed without any warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package party.morino.moripautils.common.config

import com.typesafe.config.ConfigException
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigParseOptions
import com.typesafe.config.ConfigRenderOptions
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.hocon.Hocon
import kotlinx.serialization.hocon.decodeFromConfig
import kotlinx.serialization.hocon.encodeToConfig
import party.morino.moripautils.common.model.config.MoripaUtilsConfig
import java.nio.file.Files
import java.nio.file.Path

/**
 * プラグインのデータフォルダから config.conf (HOCON 形式) を読み込むローダー
 *
 * データフォルダの場所はプラットフォーム (Paper / Velocity) ごとに異なるため、
 * Koin ではなくプラットフォーム側のプラグインクラスがコンストラクタ引数で渡して生成する。
 * common にはロガーがないので、ログ出力は行わず戻り値と例外だけで結果を伝える。
 *
 * @property dataDirectory config.conf を配置するプラグインのデータフォルダ
 * @property defaults ファイルが存在しない場合に書き出して利用する既定値 (既定ポートや server はプラットフォームごとに異なる)
 */
@OptIn(ExperimentalSerializationApi::class)
class MoripaUtilsConfigLoader(
    private val dataDirectory: Path,
    private val defaults: MoripaUtilsConfig,
) {
    /** 読み書きの両方で使う HOCON 設定 (初回生成時に全項目が並ぶよう既定値も書き出す) */
    private val hocon = Hocon {
        encodeDefaults = true
    }

    /** 設定ファイルのパス */
    private val configFile: Path = dataDirectory.resolve(CONFIG_FILE_NAME)

    /**
     * 設定を読み込む
     *
     * config.conf が存在しない場合はデータフォルダを作成し、既定値を書き出してそのまま返す。
     * 存在する場合はファイルを HOCON としてパースし、省略された項目は既定値で補ってデコードする。
     *
     * @return 読み込んだ設定 (ファイルがなければ既定値)
     * @throws IllegalStateException config.conf の構文が不正、またはデコードや値の検証 (init ブロック) に失敗した場合
     * @throws java.io.IOException ファイルの読み書きに失敗した場合
     */
    fun load(): MoripaUtilsConfig {
        // ファイルがなければ既定値を書き出して初回起動時のテンプレートにする
        if (Files.notExists(configFile)) {
            Files.createDirectories(dataDirectory)
            Files.writeString(configFile, render(defaults))
            return defaults
        }

        val text = Files.readString(configFile)
        return try {
            // エラーメッセージにファイルのパスが出るよう origin を設定し、${...} の置換も解決しておく
            val parseOptions = ConfigParseOptions.defaults().setOriginDescription(configFile.toAbsolutePath().toString())
            val config = ConfigFactory.parseString(text, parseOptions).resolve()
            hocon.decodeFromConfig(MoripaUtilsConfig.serializer(), config)
        } catch (e: ConfigException) {
            // HOCON の構文エラーや置換の解決失敗 (Typesafe Config の例外は IllegalArgumentException ではない)
            throw IllegalStateException("Failed to parse config file: ${configFile.toAbsolutePath()} (${e.message})", e)
        } catch (e: IllegalArgumentException) {
            // SerializationException と init ブロックの require 失敗は IllegalArgumentException のサブクラスなので、ここでまとめて捕捉する
            // どのファイルが壊れているか分かるように絶対パスをメッセージに含める
            throw IllegalStateException("Failed to parse config file: ${configFile.toAbsolutePath()} (${e.message})", e)
        }
    }

    /**
     * 設定を config.conf に書き出す HOCON 文字列へ変換する
     *
     * @param config 書き出す設定
     * @return 人が編集しやすい整形済みの HOCON 文字列 (JSON 形式ではなく、内部的な origin コメントは含めない)
     */
    fun render(config: MoripaUtilsConfig): String {
        val renderOptions = ConfigRenderOptions.defaults()
            .setOriginComments(false)
            .setJson(false)
            .setComments(true)
            .setFormatted(true)
        val body = hocon.encodeToConfig(MoripaUtilsConfig.serializer(), config).root().render(renderOptions)
        // 書き出したファイルが何の設定か分かるよう、先頭に案内のコメントを付ける
        return HEADER + body
    }

    companion object {
        /** 設定ファイルの名前 */
        const val CONFIG_FILE_NAME = "config.conf"

        /** 書き出す設定ファイルの先頭に付けるコメント */
        private const val HEADER = "# MoripaUtils configuration (HOCON)\n# https://utils.plugin.morino.party/docs/config\n"
    }
}
