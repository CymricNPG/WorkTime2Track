package net.npg.wt2t.domain.usecase

import net.npg.wt2t.data.model.AppSettings
import net.npg.wt2t.data.repository.ConfigRepository

/** Loads and saves the validated application configuration. */
class ConfigurationUseCase(
    private val configRepository: ConfigRepository,
) {
    suspend fun getInt(setting: AppSettings): Int =
        setting.parseIntOrDefault(configRepository.getString(setting.name, setting.defaultValue))

    suspend fun getString(setting: AppSettings): String =
        setting.parseStringOrDefault(configRepository.getString(setting.name, setting.defaultValue))

    suspend fun save(values: Map<AppSettings, String>) {
        require(values.keys == AppSettings.entries.toSet()) {
            "A complete configuration must contain every supported setting."
        }
        values.forEach { (setting, value) ->
            require(setting.isValidValue(value)) { "Invalid value for configuration setting ${setting.name}." }
        }
        configRepository.setAll(values.mapKeys { (setting, _) -> setting.name })
    }
}
