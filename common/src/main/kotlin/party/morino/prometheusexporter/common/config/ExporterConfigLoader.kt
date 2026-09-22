/*
 * Written in 2026 by Nikomaru <nikomaru@nikomaru.dev>
 *
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide.This software is distributed without any warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package party.morino.prometheusexporter.common.config

import kotlinx.serialization.json.Json
import party.morino.prometheusexporter.common.model.config.ExporterConfig
import java.nio.file.Files
import java.nio.file.Path

/**
 * プラグインのデータフォルダから config.json を読み込むローダー
 *
 * データフォルダの場所はプラットフォーム (Paper / Velocity) ごとに異なるため、
 * Koin ではなくプラットフォーム側のプラグインクラスがコンストラクタ引数で渡して生成する。
 * common にはロガーがないので、ログ出力は行わず戻り値と例外だけで結果を伝える。
 *
 * @property dataDirectory config.json を配置するプラグインのデータフォルダ
 * @property defaults ファイルが存在しない場合に書き出して利用する既定値 (既定ポートはプラットフォームごとに異なる)
 */
class ExporterConfigLoader(
    private val dataDirectory: Path,
    private val defaults: ExporterConfig,
) {
    /** 読み書きの両方で使う JSON 設定 (既定値も書き出し、未知のキーは無視する) */
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    /** 設定ファイルのパス */
    private val configFile: Path = dataDirectory.resolve(CONFIG_FILE_NAME)

    /**
     * 設定を読み込む
     *
     * config.json が存在しない場合はデータフォルダを作成し、既定値を書き出してそのまま返す。
     * 存在する場合はファイルをデコードして返す。
     *
     * @return 読み込んだ設定 (ファイルがなければ既定値)
     * @throws IllegalStateException config.json の内容をデコードできなかった場合、または値が不正 (例: samplingIntervalTicks が 0 以下) な場合
     * @throws java.io.IOException ファイルの読み書きに失敗した場合
     */
    fun load(): ExporterConfig {
        // ファイルがなければ既定値を書き出して初回起動時のテンプレートにする
        if (Files.notExists(configFile)) {
            Files.createDirectories(dataDirectory)
            Files.writeString(configFile, json.encodeToString(ExporterConfig.serializer(), defaults))
            return defaults
        }

        val text = Files.readString(configFile)
        return try {
            json.decodeFromString(ExporterConfig.serializer(), text)
        } catch (e: IllegalArgumentException) {
            // SerializationException は IllegalArgumentException のサブクラスなので、ここでまとめて捕捉する
            // どのファイルが壊れているか分かるように絶対パスをメッセージに含める
            throw IllegalStateException("Failed to parse config file: ${configFile.toAbsolutePath()}", e)
        }
    }

    companion object {
        /** 設定ファイルの名前 */
        const val CONFIG_FILE_NAME = "config.json"
    }
}
