#!/bin/bash

###
# Usage:
# > sh ./dump_demodata.sh
###

python /app/src/manage.py dumpdata \
    accounts.user \
    authtoken.token \
    producttypen \
    producten \
    locaties --indent 2 --format json > /app/demodata/demodata.json
