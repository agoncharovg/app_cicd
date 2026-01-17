# Требования к тестам

## Расположение и имя файла теста
Тест должен находиться в файле с расширением `.py`, имя которого начинается с `test_` и который располагается  
в одной из подпапок папки `tests/`. Подпапка определяется типом продукта и типом степа (`be` для backend или `fe` для UI).
Например, тесты продукта IAM:
- 'tests/iam/be' - папка для backend тестов
- 'tests/iam/be/accese_control' - папка для backend тестов, проверяющих работу access control
- 'tests/iam/fe' - папка для UI тестов
- 'tests/iam/fe/admin_portal' - папка для UI тестов, проверяющих работу админского портала 

P.S. Конкретный модуль следует указывать в теле задания на создание теста.

## Марки

Каждый модуль содержит список марок, описанных в параметре pytestmark, который используется для обеспечения возможности
разбиения тестов на группы со своим собственным алгоритмом запуска в Jenkins:

```python
pytestmark = [
    backend_test_mark,
    pytest.mark.access_control_admin_policy,
    pytest.mark.access_control_admin,
    pytest.mark.access_control,
    pytest.mark.product_iam,
]
```

Имена марок могут иметь произвольное наименование, но должны отображать главную идею тех тестов, к которым относятся.
Обязательным условием является наличие:
- марки продукта, например `product_iam` (название марки начинается с `product_` и следом идет тип продукта)
- для backend тестов марки `backend_test_mark`
- для UI тестов - `ui_test_mark`

## Декоратор окружений
Тест должен иметь декоратор `@allowed_envs`, в котором в виде списка указывается перечень окружений, в рамках которых  
данный тест может запускаться. Требуется как минимум одно окружение. 
Структуру декоратора `@allowed_envs` и полный список окружений `EnvironmentTypes` можно получить в модуле 
[`framework/core/environment.py`](../framework/core/environment.py).

Пример:
```python
@allowed_envs(envs_types=[EnvironmentTypes.PREPROD, EnvironmentTypes.CUSTOM])
```

## Docstring теста
Тест должен начинаться с docstring, в которой:
- сначала указывается краткое описание того, что тест проверяет;
- затем, через пустую строку — описание бизнес-логики теста в виде последовательных шагов.

Пример:
```python
"""
The test checks the notification about successful and failed payment transactions.

1. Prepare the notification data
2. Send the notification "billing_payment_successful_v2"
3. Assert there is an email with the expected body
"""
```

Описание бизнес-логики берется из комментариев в теле теста, которые предшествуют вызову функции, реализующей конкретный
шаг бизнес-логики. Эти коментарии в теле теста обязательны.
Каждый шаг должен быть описан в краткой форме, максимально раскрывая смысл описываемого шага операции, и понятной в том 
числе сотрудникам без опыта работы с фреймворком QAA.
Каждый шаг должен начинаться с порядкового номера (первый стартует с 1) с точкой после него и следующий одним пробелом
непосредственно перед текстом описания.
Допускается использование суб-степов, если это как упрощает чтение бизнес-логики, так и навигацию по телу теста.
Пример:
```python
@allowed_envs(envs_types=[EnvironmentTypes.PREPROD])
def test_reseller_cancel_delete_account_request(
    common_qaa_user: CommonAPIClient, driver: UnionWebDriver, driver_2: UnionWebDriver
):
    """
    Test checks the deletion account request can be canceled in the reseller's admin portal

    1. Client portal
        1.1. Sign in to the client portal with client's email and password
        1.2. Open user's profile page
        1.3. Click on the "Account profile" menu item
        1.4. Click on the "Delete account" menu item
        1.5. Delete the account
    2. Reseller's admin portal
        2.1. Sign in to the reseller's admin portal with email and password
        2.2. Open the "Accounts" page
        2.3. Set filter on the list of clients
        2.4. Edit the client
        2.5. Open the "Delete account" page
        2.6. Assert the account status is "In progress"
        2.7. Cancel the deletion of account
        2.8. Assert the account status is "Inactive"
    3. Client portal
        3.1. Refresh the page
        3.2. Assert account can be deleted again (there is no deleting request)
    """

    reseller: CommonAPIClient = UserClientProvider().qaa_reseller

    # 1. Client portal
    # 1.1. Sign in to the client portal with client's email and password
    client_portal = ClientPortal(driver)
    client_portal.login(common_qaa_user.email, common_qaa_user.password)

    # 1.2. Open user's profile page
    client_portal.header.open_profile()

    ...

    # 2. Reseller's admin portal
    # 2.1. Sign in to the reseller's admin portal with email and password
    admin_portal = AdminPortal(driver_2)
    admin_portal.login(Credentials(username=reseller.email, password=reseller.password))
```

В этом случае в docstring такой суб-степ начинается с отступа по отношению к основному степу на величину tab, и 
нумеруется порядковым номером суб-степа для данного степа, стартующего с 1 с завершающейся точкой.
Требуется следовать правилу `не более двух вложения суб-степов`, стремясь обходится без них, где это возможно.

Все описания, касающиеся внутренней работы фреймворка, не должны попадать в docstring и могут быть оформлены в коде в виде комментариев.

## Параметризация тестов

Если один тест должен выполнять одну и ту же последовательность действий для различных входных параметров  
(например, разные типы реселлеров, продуктов и т.п.), то тест может иметь одно тело, а параметры указываться  
через pytest-параметризацию.

Пример:
```python
@pytest.mark.parametrize("reseller_type", [ResellerTypes.GCORE, ResellerTypes.NON_BRANDED])
def test_notification_about_cloud_limit_change_request_support(reseller_type: ResellerTypes)
```

## Параметры тестовой функции
Параметрами тестовой функции могут быть:
- значения, указанные в параметризации;
- функции-фикстуры.

Пример:
```python
@allowed_envs(envs_types=[EnvironmentTypes.PREPROD])
@pytest.mark.parametrize("reseller_type", [ResellerTypes.GCORE, ResellerTypes.NON_BRANDED])
def test_access_control_admin_policy(reseller_type: ResellerTypes, common_gcore_permanent_user: CommonAPIClient)
```

## Фикстуры

Функции-фикстуры должны храниться в файле `conftest.py`.

Файл может быть:
- общим: [`tests/conftest.py`](../tests/conftest.py)
- для конкретного продукта: [`tests/billing/conftest.py`](../tests/billing/conftest.py)
- для конкретного типа тестов продукта: [`tests/iam/be/conftest.py`](../tests/iam/be/conftest.py)
- в прочих подпапках папки `tests/`, если есть необходимость дополнительно выделить их в обобщенную группу (`tests/iam/be/access_control/conftest.py`)

## Тело теста

Тело теста должно состоять только из функций, которые:
- подготавливают данные (data-функции, специфичные для конкретного теста [`ai_integration/2_3_data_function_rules.md`](./2_3_data_function_rules.md), 
или helper-функции, используемые в любом месте фреймворка [`ai_integration/2_2_helper_rules.md`](./2_2_helper_rules.md));
- выполняют бизнес-логику - степы [`ai_integration/2_4_1_step_be_rules.md`](./2_4_1_step_be_rules.md) и [`ai_integration/2_4_2_step_ui_rules.md`](./2_4_2_step_ui_rules.md);
- проверяют, что текущее состояние инфраструктуры соответствует ожидаемому - assert [`ai_integration/2_4_1_step_be_rules.md`](./2_4_1_step_be_rules.md);

Все такие функции должны располагаться за пределами тестового модуля.

Допускается определение в теле теста простых переменных атомарных базовых типов (`int`, `str`, `float`, `bool`), если это 
улучшает читаемость, в том числе случайно генерируемых с помощью faker.

Пример:
```python
from framework.generators import faker

def test_access_control_admin_policy(common_gcore_permanent_user: CommonAPIClient):
    client_id: int = common_gcore_permanent_user.client_id
    
    # 3. Select any domain
    random_free_domain: FreeDomainModel = faker.random_element(free_domains)

    # 4. Try to make GET /free_domains/{id} by the ID from the p.3
    loaded_free_domain: FreeDomainModel = get_free_domain(random_free_domain.id)
    
    ...
    
    # 8. Assert that client status is expected
    assert_client_status(client_id=client_id, expected_status=ClientStatusType.PREPARATION)

    # 9. Assert client status in billing
    assert_client_billing_status(
        client_id=client_id, active=True, step_name="Assert client is active in billing."
    )
```

В теле теста запрещен хардкод, когда константные значения (строки, ключи, числа) указываются в выражениях "как есть". 
Такие значения должны быть определены в виде констант с именами, позволяющими однозначно понять, что означает то или 
иное значение.
Такие константы должны располагаться:
- в начале теста после docstring, если они используются только этим тестом
- в начале модуля до первого теста, если они используются несколькими тестами в этом модуле
- в одном из файлов, описывающих используемые в фреймворке структуры (файле с именем `data_structs.py`)

Файл `data_structs.py` может быть:
- общим: [`steps/data_structs.py`](../steps/data_structs.py)
- для конкретного продукта: [`steps/iam/data_structs.py`](../steps/iam/data_structs.py)
- для конкретного типа тестов продукта: [`steps/iam/be/data_structs.py`](../steps/iam/be/data_structs.py)
- в прочих подпапках папки `steps/`, если есть необходимость дополнительно выделить их в обобщенную группу [`steps/iam/be/api_validation/data_structs.py`](../steps/iam/be/api_validation/data_structs.py)
