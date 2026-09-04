# Кодогенерация для модуля `database`

KSP-процессоры, генерирующие вспомогательный код для JPA-сущностей `testsys-infra/database`.
Результат лежит в `build/generated/ksp/main/kotlin` и в репозиторий не попадает.

| Модуль        | Содержимое                                | Подключение к `database` |
|---------------|-------------------------------------------|--------------------------|
| `codegen-api` | Аннотация `@CompositeKeyConstructor`      | `implementation(...)`    |
| `codegen`     | Процессоры, `ksp-api`, `kotlinpoet`, тесты | `ksp(...)`               |

Модули не сливать: KSP не видит `compileOnly`-зависимости, а `implementation` объединённого модуля
затащит `kotlinpoet` в рантайм. Аннотация имеет `@Retention(SOURCE)` и в байткоде отсутствует.

## `@CompositeKeyConstructor`

Генерирует для `CompositeJpaEntity<T>` top-level функцию с именем класса, принимающую поля ключа `T` напрямую.
Класс должен иметь единственный параметр конструктора `id: T`, а `T` быть `data class` из `val`-полей.
Нарушение является ошибкой компиляции.

```kotlin
// Исходник
@Embeddable
data class TaskToContestId(val taskId: Long, val contestId: Long) : CompositeId

@Entity
@CompositeKeyConstructor
class TaskToContestJpaEntity(id: TaskToContestId) : CompositeJpaEntity<TaskToContestId>(id)

// Сгенерировано: TaskToContestJpaEntity$Constructors.kt
@InternalDatabaseApi
public fun TaskToContestJpaEntity(taskId: Long, contestId: Long): TaskToContestJpaEntity =
    TaskToContestJpaEntity(TaskToContestId(taskId = taskId, contestId = contestId))

// Использование
TaskToContestJpaEntity(taskId = taskId, contestId = contestId)
```

## `<Entity>Fields`

Аннотации не требует: для каждого `@Entity` генерируется объект со строковыми именами свойств
для `Specification` и `Sort`. Включает свойства суперклассов, исключает `@Transient`, имена переводит
в `SCREAMING_SNAKE_CASE`. Для `CompositeJpaEntity<T>` добавляет вложенный `object Id` по полям ключа.
Сборку не ломает никогда.

```kotlin
// Сгенерировано: TaskToContestJpaEntity$Fields.kt
public object TaskToContestJpaEntityFields {
    public const val CREATED_AT: String = "createdAt"
    public const val UPDATED_AT: String = "updatedAt"
    public const val VERSION: String = "version"
    public const val ID: String = "id"

    public object Id {
        public const val TASK_ID: String = "taskId"
        public const val CONTEST_ID: String = "contestId"
    }
}

// Использование
root.get<Any>(TaskToContestJpaEntityFields.ID).get<Long>(TaskToContestJpaEntityFields.Id.CONTEST_ID)
```

## Тесты

`codegen/src/test`, на `kctfork-ksp`: компилируют фрагмент с процессором и проверяют сгенерированный текст.
Базовые классы и `jakarta.persistence` подменены заглушками. Пакет заглушек обязан совпадать с реальным,
иначе процессор не найдёт супертип, а тест этого не заметит.

```bash
./gradlew :testsys-infra:database:codegen:test
```
