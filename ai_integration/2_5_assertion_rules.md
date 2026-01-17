# Проверяющие функции (Assert-функции)

Assert-функции - это разновидность степов, которые выполняют проверку текущего состояния инфраструктуры и выносят 
решение, может ли тест продолжать работу или должен завершить ее в связи с ошибкой проверки.

## Расположение assert-функций

Assert-функции хранятся в модулях с именем `assertions.py` в папке `steps/`, который может быть:
- общим: [`steps/assertions.py`](../steps/assertions.py)
- для конкретного продукта: [`steps/cdn/assertions.py`](../steps/cdn/assertions.py)
- для проверки backend API - располагаются рядом с соответствующим файлом `api.py`
- для проверки на web-страницах - располагаются в классе, описывающем соответствующую web-страницу

Пример backend assert-функции:
[`steps/iam/be/access_control/services/assertions.py](../steps/iam/be/access_control/services/assertions.py)
```python
@api_step_iam(name="Assert access control allows use of the product", tries=14, delay=5)
def assert_access_control_allows_use_product(user_id: int, reseller_id: int, product_name: str):
    permissions: list[AccessControlServicePermissionModel] = get_access_control_service_permissions_for_user(user_id)
    entities = {USER_ID: user_id, RESELLER_ID: reseller_id, "Product": product_name}
    for perm in permissions:
        assert not [item for item in perm.effects.deny if item.product == product_name.lower()], AssertMessage(
            fail_reason="Product is denied for the user", entities=entities
        )

    for perm in permissions:
        if [item for item in perm.effects.allow if item.product == product_name.lower()]:
            assert perm.reseller_id == reseller_id, AssertMessage(
                fail_reason="Permission to use the product is, but reseller is not expected", entities=entities
            )
            return

    raise AssertionError(AssertMessage(fail_reason="Product is now allowed for the user", entities=entities))
```

Пример UI assert-функции:
[`steps/iam/fe/pages/client/pages_client_profile.py](../steps/iam/fe/pages/client/pages_client_profile.py)
```python
class ClientProfileMyProfile(BaseWebPage):
    ...
    
    @ui_step_iam(name="Assert, that email is expected")
    def assert_email_is_expected(self, expected_email: str):
        alert_text: str = self.general_alert_text.get_text()
        assert expected_email in alert_text, UIAssertMessage(
            self.driver,
            fail_reason=FailReason.UI.UNEXPECTED_ELEMENT_VALUE,
            entities={
                EXPECTED: expected_email,
                ACTUAL: alert_text,
            },
        )
```

### Общие правила описания assert-функций

#### Наименование функции

Имя функции должно начинаться только с `assert_` и быть обернуто в соответствующий декоратор степа. Как и в случае со степами
бизнес логики, наименование функции должно быть максимально похоже на описание степа, который должен начинаться с `Assert `.
Пример:
```python
@api_step_iam(name="Assert the access control client policy can be deleted")
def assert_access_control_client_policy_can_be_deleted(client: CommonAPIClient, policy_id: str):
    details: AccessControlDeletionDetailsModel = delete_access_control_client_policy(client=client, policy_id=policy_id)

    assert details.detail == (expected_detail := AccessControlDeletionDetailTypes.DELETED), AssertMessage(
        fail_reason=FailReason.IAM.AccessControl.UNEXPECTED_RESULT_DELETE_CLIENT_POLICY,
        entities={
            "Policy ID": policy_id,
            EXPECTED: expected_detail,
            ACTUAL: details.detail,
        },
    )
```

#### Ретраи assert-функций

При осуществлении проверки состояния инфраструктуры принимается, что оно может меняться не моментально, а со временем.
Максимальное значение этого времени может быть предоставлено разработчиками.
При этом используется алгоритм ретраев, инкапсулированный в декоратор степа и позволяющий управлять как количеством
повторных попыток, так и паузами между ними.
Пример:
```python
@api_step_iam(name="Assert that service is enabled.", exceptions=AssertionError, tries=5, delay=6)
def assert_service_is_enabled(client_id: int, service_type: str, enabled: bool = True) -> None:
    service_data: ClientServiceDataModel = get_client_service(
        client_id=client_id, service_type=service_type, step_delay=1, step_tries=1
    )
    assert (actual_status := service_data.enabled) == enabled, AssertMessage(
        fail_reason="Unexpected service enabled status",
        entities={
            CLIENT_ID: client_id,
            SERVICE_TYPE: service_type,
            EXPECTED: enabled,
            ACTUAL: actual_status,
        },
    )
```

#### Вложенность

Assert-функция может быть комплексной и содержать вложенные степы как бизнес процессов (напрмер, запросы API), так и 
другие assert функции. При этом должно соблюдаться условие, что вложенные степы не должны оборачиваться в try..except. 
Т.е. падение вложенного степа однозначно ведет к падению родительского степа.

Если все же требуется использование try..except, то необходимо переформатировать вызываемую внутри блока функцию так,
чтобы она не содержала ни одного степа и поместить ее в модуль helpers.py.

Т.к. и родительский и вложенные степы допускают использование ретраев (tries+delay), то требуется избегать их в главном
степе. Т.е. в идеале, главный степ должен иметь одноразовое выполнение.
Если соблюсти это условие невозможно и главный степ тоже должен иметь возможность повторов, то следует либо добавить 
логику, либо соблюсти баланс по максимальному времени выполнения главного степа, если предположить, что он в итоге
закончится неуспехом.

#### Способ проверки

Проверка осуществляется командой `assert`, при этом аргументом исключения AssertionError должнен использоваться:
- для backend степов - объект класса AssertionMessage;
- для UI степов - объект класса UIAssertionMessage

Пример backend проверки:
```python
@api_step_iam(name="Assert that service is enabled.", exceptions=AssertionError, tries=5, delay=6)
def assert_service_is_enabled(client_id: int, service_type: str, enabled: bool = True) -> None:
    service_data: ClientServiceDataModel = get_client_service(
        client_id=client_id, service_type=service_type, step_delay=1, step_tries=1
    )
    assert (actual_status := service_data.enabled) == enabled, AssertMessage(
        fail_reason="Unexpected service enabled status",
        entities={
            CLIENT_ID: client_id,
            SERVICE_TYPE: service_type,
            EXPECTED: enabled,
            ACTUAL: actual_status,
        },
    )
```

Пример UI проверки:
```python
@ui_step_iam(name="Assert quick search product panel was expanded", tries=5, delay=2)
def assert_quick_search_product_panel_expanded(self, previous_card_count: int):
    card_count: int = self.quick_search_product_panel_count
    assert previous_card_count < card_count, UIAssertMessage(
        self.driver,
        fail_reason="Unexpected number of cards were shown",
        entities={
            EXPECTED: previous_card_count,
            ACTUAL: card_count,
        },
    )    
```

### Структура сообщения об ошибке (backend)

Данная структура описана в классе `AssertMessage`, который описан в модуле [`framework/core/assert_message.py`](../framework/core/assert_message.py).

#### Fail reason

Обязательным для заполнения является параметр `fail_reason`, в котором указывается краткое и понятное описание причины ошибки.
Fail reasons следует в первую очередь сопостовлять со коллекцией, хранящейся в классе `FailReason` в модуле[`framework/data_structs/fail_reasons.py`](../framework/data_structs/fail_reasons.py).
И только в случае отсутствия там подходящего варианта, добавлять новый.

#### Entities
Подробные детали ошибки должны быть указаны в параметре `entities`, являющимся dict. При этом в качестве общеупотребимых
ключей стоит использовать константы, такие как:
- EXPECTED, ACTUAL, ..., EXPECTED_ERROR_TEXT в модуле [`steps/data_structs.py](../steps/data_structs.py)
- CLIENT_ID, USER_ID, RESELLER_ID, SELLER_ID в модуле [`steps/iam/data_structs.py](../steps/iam/data_structs.py)

P.S. TODO Здесь нужно доработать эту тему


### Структура сообщения об ошибке (UI)

Данная структура описана в классе `UIAssertMessage`, который описан в модуле [`framework/core/ui_assert_message.py`](../framework/core/ui_assert_message.py).

#### Driver

В обязательно параметре `driver` передается объект, который обеспечивает взаимодействие с фреймворком Playwright.
Он же является обязательным параметров для всех классов, описывающих web-страницы.
Пример:
```python
class ClientProfileMyProfile(BaseWebPage):
    def __init__(self, driver: UnionWebDriver):
        super().__init__(driver)
        self.table = GCSimpleTable(driver, product=Product.IAM)

    @ui_step_iam(name="Assert, that email is expected")
    def assert_email_is_expected(self, expected_email: str):
        alert_text: str = self.general_alert_text.get_text()
        assert expected_email in alert_text, UIAssertMessage(
            self.driver,
            fail_reason=FailReason.UI.UNEXPECTED_ELEMENT_VALUE,
            entities={
                EXPECTED: expected_email,
                ACTUAL: alert_text,
            },
        )
```

#### Fail reason

Обязательным для заполнения является параметр `fail_reason`, в котором указывается краткое и понятное описание причины ошибки.
Fail reasons следует в первую очередь сопостовлять со коллекцией, хранящейся в классе `FailReason` в модуле[`framework/data_structs/fail_reasons.py`](../framework/data_structs/fail_reasons.py).
И только в случае отсутствия там подходящего варианта, добавлять новый.

#### Entities
Подробные детали ошибки должны быть указаны в параметре `entities`, являющимся dict. При этом в качестве общеупотребимых
ключей стоит использовать константы, такие как:
- EXPECTED, ACTUAL, ..., EXPECTED_ERROR_TEXT в модуле [`steps/data_structs.py](../steps/data_structs.py)
- CLIENT_ID, USER_ID, RESELLER_ID, SELLER_ID в модуле [`steps/iam/data_structs.py](../steps/iam/data_structs.py)

P.S. TODO Здесь нужно доработать эту тему

