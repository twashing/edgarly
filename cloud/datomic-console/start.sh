#!/usr/bin/env bash

sed "/host=0.0.0.0/a alt-host=${ALT_HOST:-127.0.0.1}" -i config/dev-transactor.properties

bin/console -p 8080 dev datomic:dev://${DATOMIC_HOST}:4334
