# UI степ

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
- attach_screenshots: bool = True - признак необходимости прикрепления скриншота экрана к аллюр отчету

Имя функции и имя степа должны быть похожи.

Пример:
```python
@ui_step_cdn(name="Fill Gcore provider fields")
def fill_gcore_provider(self, storage_data: StorageData, bucket_name: str, scheme: str = Scheme.HTTPS):
    self.storage_provider_gcore_radio_button.click()
    ...
```

## Универсальный UI степ

Универсальный UI степ - это степ, который может использоваться разными продуктами и имеет имя `ui_step`.
Пример:
```python
@ui_step(name="Open the browser tab.")
def open_browser_tab(self, index: int):
    self.driver.open_page(index)
```

```python
open_browser_tab(index=1, product=Product.IAM)
```

При вызове функции универсального степа, если он не является вложенным, всегда требуется указывать тип продукта в
параметре `product`, чтобы, в случае возникновения ошибки, информация о ней отправилась в подписчикам именно этого
продукта, а не в общий канал QAA команды.
Если универсальный степ вызывается из другого, но продут-ориентированного степа, тип продукта указывать необязательно,
т.к. в этом случае информация об ошибке будет отправлена подписчикам того, продукта, из степа которого происходит вызов.


## Продукт-специфичный UI степ

Продукт-специфичный UI степ применяется для обозначения функции, как однозначно выполняющей оперцию с конкретным продуктом.
Имя такого степа начинается с `ui_step_` и завершается именем продукта, например: 
`ui_step_iam`, `ui_step_billing`, `ui_step_qaa`, ...
Описание всех продукт-специфичных backend степов доступно в [`framework/core/step.py`](../framework/core/step.py).

Пример продукт-специфичного степа:
```python
@ui_step_iam(name="Close the banner")
def close_banner_if_exists(self):
    CookiesBanner(self.driver).close_banner_if_exists()
```

Вызов:
```python
CookiesBanner(self.driver).close_banner_if_exists()
```

Следует всегда применять продукт-специфичные декораторы степа, т.к. этим избегается избыточное явное указание 
типа продукта при каждом вызове функции степа и улучшается читабельность кода.
Универсальный декоратор допускается только в случае, если этот степ является общим для всего фреймворка и может быть 
вызван для любого его продукта.

## Область применения

UI степы используются только в UI тестах и служат для web-серфинга по страницам ресурса. 

## Расположение UI степов

Каждая web-страница описывается специальным классом, инкапсулирующим в себе всю необходимую информацию:
- URL страницы
- список локаторов, использумых в тестах
- UI степы бизнес-процессов
- проверяющие UI функции

Т.е. все, что касается конкретной страницы, находится в ее классе.

P.S. TODO Подробнее о правилах работы с UI классами - в модуле [`ai_integration/2_ui_classes_rules.md`](./2_ui_classes_rules.md)

Описания web-страниц, соответственно и UI степы, хранятся в модулях в файлах `steps/<product_type>/fe/pages/page_<description>.py`, где:
- product_type - это тип соответствующего продукта
- description - пояснительное описание хранящихся в модуле классов web-страниц  

Пример:
[`steps/iam/fe/pages/client/pages_client_dashboard.py`](../steps/iam/fe/pages/client/pages_client_dashboard.py)
```python
class ClientDashboardPage(BaseWebPage):
    def __init__(self, driver: UnionWebDriver):
        super().__init__(driver)
        self.user_profile = self.UserProfileWidget(driver)
    
    ...
    
    @ui_step_iam(name="Get the welcome product card on the dashboard", tries=5, delay=2)
    def welcome_product_card(self, product_card_type: str, is_gcore_reseller: bool = True) -> IAMClickableElement:
        element: PlaywrightElement | None = next(
            (
                item
                for item in self.driver.find_elements(
                    **self.Locators.welcome_product_card.set_dynamic_args(product_card_type).dict
                )
            ),
            None,
        )
        ...
```

## Вложенные степы

Степ может быть комплексным и содержать вложенные степы. При этом должно соблюдаться условие, что вложенные степы
не должны оборачиваться в try..except. Т.е. падение вложенного степа однозначно ведет к падению родительского степа.

Если все же требуется использование try..except, то необходимо использовать внутри блока функцию-helper, поместив ее 
в модуль `helpers.py`.

Т.к. и родительский, и вложенные степы допускают использование ретраев (tries+delay), то требуется избегать их в главном
степе. Т.е. главный степ должен иметь одноразовое выполнение.
Если соблюсти это условие невозможно и главный степ тоже должен иметь возможность повторов, то следует соблюсти баланс 
по максимальному времени выполнения главного степа в случае, когда он может закончится неуспехом.
