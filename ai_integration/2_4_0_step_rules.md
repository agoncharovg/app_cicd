# Функции, выполняющие бизнес-логику (степ-функции)

## Общая информация

В зависимости от назначения, степы делятся на backend и frontend (UI).
Тип степа определяется по обязательному декоратору, прикрепленному к степу. 
Backend степ начинается с `api_step_`, UI степ - с `ui_step_`.
Степы могут содержать в себе: 
- backend - только backend подстепы [`ai_integration/2_4_1_step_be_rules.md`](./2_4_1_step_be_rules.md)
- UI - как UI, так и backend подстепы [`ai_integration/2_4_1_step_fe_rules.md`](./2_4_2_step_fe_rules.md)

