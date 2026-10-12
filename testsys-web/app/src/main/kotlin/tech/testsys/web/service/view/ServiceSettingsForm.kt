package tech.testsys.web.service.view

import com.vaadin.flow.data.binder.Binder
import tech.testsys.infra.settings.api.SettingsSnapshot
import tech.testsys.infra.settings.api.catalog.SettingKey
import tech.testsys.web.components.forms.ValueInput

internal class ServiceSettingsForm(private val initial: SettingsSnapshot) {
    private val binder = Binder<MutableMap<SettingKey, String>>()
    private val values = SettingKey.entries.associateWith { key -> initial.values[key] }.toMutableMap()
    private val fields = mutableMapOf<SettingKey, ValueInput<String>>()

    fun bind(key: SettingKey, field: ValueInput<String>) {
        fields[key] = field
        binder.forField(field).withValidator({ value -> value.isNotBlank() }, "Укажите значение")
            .bind({ bean -> bean.getValue(key) }, { bean, value -> bean[key] = value })
        field.value = initial.values[key]
    }

    fun changes(): Map<SettingKey, String>? {
        fields.values.forEach { field -> field.isInvalid = false }
        if (!binder.writeBeanIfValid(values)) return null
        return values.filter { (key, value) -> value != initial.values[key] }
    }

    fun cancel() {
        SettingKey.entries.forEach { key -> values[key] = initial.values[key] }
        binder.readBean(values)
    }

    fun errors(errors: Map<SettingKey, String>) {
        errors.forEach { (key, _) ->
            fields.getValue(key).apply {
                errorMessage = "Проверьте формат и допустимый диапазон значения"
                isInvalid = true
            }
        }
    }
}
