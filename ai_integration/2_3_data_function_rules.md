### Data-функции

Такие функции должны начинаться с "data_" и располагаться в модулях, имя которых начинается с "data_for_test_" и 
распологающихся в заданных подпапках в папке "steps/". Точное расположение и имя файла должно указываться в описании задачи. 

Пример:
[`steps/notifications/be/data_for_tests/data_for_tests_cdn_notifications.py`](../steps/notifications/be/data_for_tests/data_for_tests_cdn_notifications.py)
```python
def data_cdn_resources_suspended_soon(reseller_type: str) -> dict:
    result = {
        KEY_NOTIFICATION_TYPE: (notification_type := Notifications.Common.CDN.SuspendingCDNResources.SUSPENDED_SOON),
        KEY_CLIENT: (client := get_permanent_notifications_client_for_reseller(reseller_type, notification_type)),
        "activity_numdays": (activity_numdays := faker.random_int(min=1, max=10)),
        "activity_day_or_days": day_or_days(activity_numdays),
        "suspended_resources_numdays": (suspended_resources_numdays := faker.random_int(min=1, max=10)),
        "suspended_day_or_days": day_or_days(suspended_resources_numdays),
        "cdn_resources_id": faker.resource_id(),
        "cdn_resources_cname": faker.resource_name(),
        "company": client.company,
        "account_id": client.client_id,
    }
    expected_email_body: list = prepare_expected_notification_mail_body(
        notification_type=notification_type,
        reseller_type=reseller_type,
        replace_args=result,
        body_ext=str(reseller_type),
    )
    return result | {KEY_EXPECTED_EMAIL_BODY: expected_email_body}
```