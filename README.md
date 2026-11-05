# kfd-dsl-lab

dsl для проверки http-ответов. шесть старых `check01`–`check06` переписаны через него: `newCheck01`–`newCheck06` в пакете `lesson09.dsl`.

## сборка и тесты

```
bash ./gradlew build
bash ./gradlew test
```

## пример

```kotlin
val result = validate(response) {
    status { equalTo(200) }
    header("Content-Type") { startsWith("application/json") }
    body {
        json {
            field("id") { present() }
        }
    }
}
println(result.describe())
```

ответ `500`, `Content-Type: text/plain`, тело `{}`:

```
status: expected 200, actual 500
headers.Content-Type: expected prefix application/json, actual [text/plain]
body.json.id: expected present, actual <missing>
```

пути: `status`, `headers.<имя>`, `body`, `body.json`, `body.json.<поле>`.

## как устроено

- `validate` на каждый вызов создаёт новый `DivergenceCollector`, гоняет лямбду и отдаёт `ValidationResult` с копией списка. поэтому запуски независимы, а старые результаты не меняются. входной ответ не трогаем
- получатели по уровням: `ResponseScope` (status, header, body) → `StatusScope` (equalTo) / `HeaderScope` (hasValue, startsWith, satisfiesOurRequirements) / `BodyScope` (nullOrEmpty, nonEmpty, json) → `JsonObjectScope` (field) → `JsonFieldScope` (present, equalTo для String, Long, Boolean)
- все получатели помечены `@JsonDsl` (`@DslMarker`), внешний блок виден только явно: `this@validate.status { }`
- свой предикат для заголовка: `(String) -> Boolean`, хватает одного подходящего значения. передаётся лямбдой, переменной или ссылкой `::fn`, результат одинаковый
- заголовки: имя без учёта регистра, ключи с разным регистром склеиваются в порядке map, значения сравниваются как есть. нет заголовка — `<missing>`, пустой список — `[]`
- json разбирается только внутри `json { }`. если тело `null` или не разобралось, одно расхождение `body.json`, поля пропускаются, остальные проверки идут дальше