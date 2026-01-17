### Функции-helper'ы

#### Расположение helpers
Helpers должны располагаться в папке `steps/` в модуле с названием `helpers.py` :
Файл `helpers.py` может быть:
- общим: [`steps/helpers.py`](../steps/helpers.py)
- для конкретного продукта: [`steps/storage/helpers.py`](../steps/storage/helpers.py)
- для конкретного типа тестов продукта: [`steps/storage/be/helpers.py`](../steps/storage/be/helpers.py)
- в прочих подпапках папки `steps/`, если есть необходимость дополнительно выделить их в обобщенную группу [`steps/iam/be/api_validation/helpers.py`](../steps/iam/be/api_validation/helpers.py)

#### Особенности helpers
- Как правило, helpers не имеют декораторов степов.
- Предполагается, что они:
  - не генерируют ошибки;
  - либо вызываются внутри других функций-степов.

Пример:
```python
def random_int_excluding(*, low: int, high: int = 100, exclude: int) -> int:
    choices = [i for i in range(low, high + 1) if i != exclude]
    return random.choice(choices)
```
