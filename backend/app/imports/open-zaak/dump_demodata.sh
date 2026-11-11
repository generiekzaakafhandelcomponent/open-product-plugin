#!/bin/bash

###
# Usage:
# > sh ./dump_demodata.sh
###

python /app/src/manage.py dumpdata \
    accounts.user \
    authorizations.applicatie \
    vng_api_common.jwtsecret \
    catalogi.catalogus \
    catalogi.zaaktype \
    catalogi.statustype \
    catalogi.resultaattype \
    catalogi.eigenschapspecificatie \
    catalogi.eigenschap \
    catalogi.roltype \
    catalogi.informatieobjecttype \
    catalogi.besluittype \
    catalogi.zaaktypeinformatieobjecttype \
    notifications_api_common.notificationsconfig \
    zgw_consumers.service \
    zaken.zaakidentificatie \
    zaken.zaak \
    zaken.rol \
    zaken.status \
    zaken.natuurlijkpersoon --indent 2 --format json > /tmp/demodata.json

# zaken.zaak.zaaktype, zaken.rol.roltype and zaken.status.statustype are a local
# FkOrServiceUrlField; for a locally stored zaaktype/roltype/statustype it serializes
# as the object's display text instead of a URL, which loaddata then rejects. The real
# relation is the `_zaaktype`/`_roltype`/`_statustype` FK id, already included, so the
# display-only field can be dropped.
python -c "
import json
with open('/tmp/demodata.json') as f:
    data = json.load(f)
for obj in data:
    if obj['model'] == 'zaken.zaak':
        obj['fields'].pop('zaaktype', None)
    if obj['model'] == 'zaken.rol':
        obj['fields'].pop('roltype', None)
    if obj['model'] == 'zaken.status':
        obj['fields'].pop('statustype', None)
with open('/app/demodata/demodata.json', 'w') as f:
    json.dump(data, f, indent=2)
"
