# Backend степ

## Декоратор степа

Степ-функция обязательно должна иметь декоратор степа.
Декоратор может быть:
- универсальным
- продукт-специфичным

## Параметры декоратора степа

- name: str — наименование степа
- tries: int — количество повторов
- delay: int — пауза между повторами
- show_lead_time: bool — отображение времени выполнения в отчёте
- exceptions: Any = Exception — исключения для ретраев

Имя функции и имя степа должны быть максимально похожи.

Пример:
```python
@api_step_cdn(
    show_lead_time=True,
    exceptions=AssertionError,
    tries=ResourceActivationProdTimeout.TRIES,
    delay=ResourceActivationProdTimeout.DELAY,
    name="Wait for resource to become in status.",
)
def wait_for_resource_to_become_in_status(
    api_client: CommonAPIClient,
    resource_id: int | str,
    expected_status: str = ResourceStatusTypes.ACTIVE,
):
    ...
```

## Универсальный backend степ

Универсальный backend степ - это степ, который может использоваться разными продуктами и имеет имя `api_step`.
Пример:
```python
@api_step(name="Get links from HTML.")
def get_links_from_html(html_body: str) -> list:
    ...
```

Вызов:
```python
get_links_from_html(html_body=html_body, product=Product.IAM)
```

При вызове функции универсального степа, если он не является вложенным, всегда требуется указывать тип продукта в
параметре `product`, чтобы, в случае возникновения ошибки, информация о ней отправилась в подписчикам именно этого
продукта, а не в общий канал QAA команды.
Если универсальный степ вызывается из другого, но продут-ориентированного степа, тип продукта указывать необязательно,
т.к. в этом случае информация об ошибке будет отправлена подписчикам того, продукта, из степа которого происходит вызов.

## Продукт-специфичный backend степ

Продукт-специфичный backend степ применяется для обозначения функции, как однозначно выполняющей оперцию с конкретным продуктом.
Имя такого степа начинается с `api_step_` и завершается именем продукта, например: 
`api_step_iam`, `api_step_billing`, `api_step_qaa`, ...
Описание всех продукт-специфичных backend степов доступно в [`framework/core/step.py`](../framework/core/step.py).

Пример продукт-специфичного степа:
```python
@api_step_iam(name="Get SAML identity providers")
def get_saml_identity_providers(params: dict | None = None) -> list[SAML2IdentityProvidersModel]:
    ...
```

Вызов:
```python
providers: list[SAML2IdentityProvidersModel] = get_saml_identity_providers()
```

Следует всегда применять продукт-специфичные декораторы степа, т.к. этим избегается избыточное явное указание 
типа продукта при каждом вызове функции степа и улучшается читабельность кода.
Универсальный декоратор допускается только в случае, если этот степ является общим для всего фреймворка и может быть 
вызван для любого его продукта.

## Область применения

Backend степы могут использоваться в обоих типах тестов - backend и UI. Их назначение - управление инфраструктурой
через API когда это нужно или, в случае с UI, может упростить тест там, где имеется избыточный серфинг по web-страницам,
прежде чем будет достигнута страница, являющаяся целью тестирования.

Пример:
если требуется протестировать фичу или опцию CDN в UI, то проходить в каждом тесте полный путь по активации сервиса и
создании CDN ресурса в UI - избыточно, т.к. это дольше и может нести риски получить по пути случайную ошибку UI
(например, какая-то из начальных web-страниц будет грузиться дольше в момент проверки, чем заложено в тест и он будет 
завершен по таймауту, даже не дойдя до страницы фичи или опции). В этом случае логично произвести подготовку с 
помощью backend, а в UI проверить только требуемую web-страницу.

Основной областью применения backend степов является взаимодействие с инфраструктурой через вызов API функций, но возможны
и прочие функции, выполняющие определенные операции и требующие необходимости быть обозначенными как степы.

## Работа с API

Вызов API функций - это HTTP запрос, реализуемый с помощью клиента, класс CommonAPIClient которого описан в
[`framework/client/clients/common_api_client.py`](../framework/client/clients/common_api_client.py).

### Расположение степов для вызова API

Степы хранятся в модулях с именем `api.py` в папке `steps/<product_type>/be/<sub_path>`, где:
- product_type - это тип соответствующего продукта
- sub_path - набор вложенных папок 

Пример:
API продукта IAM для работы с юзерами хранятся в папке [`steps/iam/be/users/api.py`](../steps/iam/be/users/api.py), 
в которой имеется дополнительное разделение по папкам для дочерних `users` эндпойнтам, например `email`, `me`.

### Клиенты

Степ, работающий с API, должен иметь в качестве одного из параметров объект класса CommonAPIClient,
если предполагается вызов с токеном клиента, реселлера или селлера. 
Если это вызов админского API, то внутри функции требуется оперировать UserClientProvider().system_admin_user, который 
так же является объектом класса CommonAPIClient.

Подробней о клиентах описано в [`docs/client.md`](../docs/client.md)

Пример backend степа для вызова API c клиентским токеном:
```python
@api_step_iam(name="Create a client's user")
def create_client_users(client: CommonAPIClient, data: dict) -> ClientUsersPostResultModel:
    response: dict = client.iam.clients(client.client_id).client_users.post(data=data, check_for=ResponseCode.CODE_200)
    return validate_data_fields(
        data=response,
        model=ClientUsersPostResultModel,
        erroneous_result="Invalid created client's user data",
        entities={"Client ID": client.client_id, "Data": data},
    )
```

Пример backend степа для вызова API c админским токеном:
```python
@api_step_iam(name="Create a reseller")
def create_reseller(data: dict) -> ResellerDataModel:
    response: dict = UserClientProvider().system_admin_user.iam.admin.resellers.post(data)
    return validate_data_fields(data=response, model=ResellerDataModel, erroneous_result="Invalid reseller data")
```

### Принцип формирования HTTP-запроса для вызова API

HTTP-запрос выполняется в виде последовательного указания всей цепочки эндпойнтов, участвующих в формировании URL запроса,
первый из которых указывает тип продукта и является частью класса CommonAPIClient. Последним элементом цепочки должен быть указан
метод выполняемого запроса со списком параметров, если они необходимы.
Требование к запросу в степе:
- один step - один HTTP-запрос
- используемые методы: GET, POST, PUT, PATCH, DELETE

Пример HTTP POST запроса по адресу `/iam/admin/resellers`:
```python
@api_step_iam(name="Create a reseller")
def create_reseller(data: dict) -> ResellerDataModel:
    response: dict = UserClientProvider().system_admin_user.iam.admin.resellers.post(data)
```

Реализации всех методов описаны в классе `Resource` в модуле [`framework/client/resources/resource.py`](../framework/client/resources/resource.py).

P.S. TODO `Подробней о принципе формирования описано в ...`

### Проверка status code

Каждый HTTP запрос возвращает в ответе status_code. В методах класса `Resource`, реализующих запросы, так же поверяется,
что статус ответа имеет ожмдаемое значение, которое указывается в параметре `expected_code` декоратора метода.
Если при вызове степа известно, что `status_code` конкретной API функции отличается от заданного по-умолчанию, 
необходимо передать это значение в именованном параметре `check_for` в используемом методе:
```python
@api_step_waap(name="Create API path.")
def create_api_path(
    waap_user: CommonAPIClient, domain_id: int, data: dict, expected_status_code: int = ResponseCode.CODE_201
) -> APIPathCreateModel:
    response: dict = waap_user.waap.v1.domains(domain_id).api_path.post(data=data, check_for=expected_status_code)
    ...
```

### Ответ без JSON

По-умолчанию ожидается, что ответ на запрос будет в формате json. Если какой-либо запрос не соответствует этому
необходимо при выполнении запроса указать в именованном параметре `extract_json` значение False:
```python
@api_step_iam(name="Set user password.")
def set_user_password(data: dict, expected_code: int = ResponseCode.CODE_200):
    UserClientProvider().system_admin_user.iam.auth.set_password.post(
        data=data, check_for=expected_code, extract_json=False
    )
```

### Управление логированием

Все методы, выполняющие HTTP запрос, по-умолчанию логируют запрос и ответ с флагом `logging.INFO`.
Это можно изменить с помощью именованного параметра `log_level`:
```python
@api_step_iam(name="Set user password.")
def set_user_password(data: dict, expected_code: int = ResponseCode.CODE_200):
    UserClientProvider().system_admin_user.iam.auth.set_password.post(
        data=data, check_for=expected_code, extract_json=False, log_level=logging.NOTSET,
    )
```

### Валидация ответа

Полученный в ответе на HTTP запрос результат в виде json валидируется на соответствие ожидаемой структуре и возвращается 
в виде pydantic модели (если результат в виде dict), или списка моделей (если результат в виде list). 
В первом случае используется функция валидирования `validate_data_fields` из модуля [`steps/be/assertions.py`](../steps/be/assertions.py), 
во втором - `validate_data_list` из того же модуля.
Модели для каждого эйндпойта описываются в модуле `models.py`, который хранится рядом с соответствующим файлом `api.py`.

Пример модели эндпойнта `/iam/users`:
```python
class UserDataModel(BaseModel):
    activated: bool
    auth_types: list[str]
    client: int | None
    company: str
    deleted: bool
    email: str
    groups: list[UserGroupModel]
    id: int
    is_active: bool
    lang: str
    name: str
    phone: str | None
    reseller: int
    social_network_auth: bool
    sso_auth: bool
    two_fa: bool
    user_type: str
    clients_and_roles: list[ClientsAndRoleModel] | None = None
```

Пример выполнения запроса и валидации ответа:
```python
@api_step_iam(name="Get user by the ID")
def get_user_by_id(user_id: int) -> UserDataModel:
    response: dict = UserClientProvider().system_admin_user.iam.users(user_id).get()
    return validate_data_fields(
        data=response,
        model=UserDataModel,
        erroneous_result="Invalid user data",
        entities={USER_ID: user_id},
    )
```

## Уникальность API запросов

Каждый эндойнт API должен вызываться единственной функцией-степом в коде фреймворка.
Если требуется получение результата выполения запроса в dict не валидированным, то следует применять следующую схему:
* запрос помещается в request-функцию без декоратора, начинающуюся на `request_`, далее следует метод запроса и вся цепочка
эндпойнтов через символ `_`; функция возвращает резульатат в dict или list;
* степ-функция с декоратором вызывает request-функцию и далее валидирует уже полученный таким образом результат

Схема request + step:
```python
def request_get_logs_uploader_target(
    client: CommonAPIClient, target_id: int
) -> dict:
    return client.cdn.logs_uploader.targets(target_id).get()

@api_step_cdn(name="Get logs uploader target")
def get_logs_uploader_target(
    client: CommonAPIClient, target_id: int,
) -> LogsUploaderTargetModel:
    response: dict = request_get_logs_uploader_target(client, target_id)
    return validate_data_fields(
        model=LogsUploaderTargetModel,
        data=response,
        erroneous_result="Invalid logs uploader target data",
        entities={
            CLIENT_ID: client.client_id,
            "Target ID": target_id,
        },
    )
```

P.S. TODO Создать документ с подробным описанием принципа описания API функций и реквестов

## Вложенные степы

Степ может быть комплексным и содержать вложенные степы. При этом должно соблюдаться условие, что вложенные степы
не должны оборачиваться в try..except. Т.е. падение вложенного степа однозначно ведет к падению родительского степа.

Если все же требуется использование try..except, то необходимо использовать внутри блока функцию-helper, поместив ее 
в модуль `helpers.py`.

Т.к. и родительский, и вложенные степы допускают использование ретраев (tries+delay), то требуется избегать их в главном
степе. Т.е. главный степ должен иметь одноразовое выполнение.
Если соблюсти это условие невозможно и главный степ тоже должен иметь возможность повторов, то следует соблюсти баланс 
по максимальному времени выполнения главного степа в случае, когда он может закончится неуспехом.
