#!/bin/bash

# https://github.com/keycloak/keycloak-containers/blob/master/server/tools/docker-entrypoint.sh#L4-L28
# usage: file_env VAR [DEFAULT]
#    ie: file_env 'XYZ_DB_PASSWORD' 'example'
# (will allow for "$XYZ_DB_PASSWORD_FILE" to fill in the value of
#  "$XYZ_DB_PASSWORD" from a file, especially for Docker's secrets feature)
file_env() {
    local var="$1"
    local fileVar="${var}_FILE"
    local def="${2:-}"
    if [[ ${!var:-} && ${!fileVar:-} ]]; then
        echo >&2 "error: both $var and $fileVar are set (but are exclusive)"
        exit 1
    fi
    local val="$def"
    if [[ ${!var:-} ]]; then
        val="${!var}"
    elif [[ ${!fileVar:-} ]]; then
        val="$(< "${!fileVar}")"
    fi

    if [[ -n $val ]]; then
        export "$var"="$val"
    fi

    unset "$fileVar"
}

# https://github.com/dcm4che-dockerfiles/wildfly/blob/1550dced41da57248c40b963706e856c67d31858/docker-entrypoint.sh#L45
# for c in $KARNAK_WAIT_FOR; do
#   echo "Waiting for $c ..."
#    while ! nc -w 1 -z ${c/:/ }; do sleep 1; done
#     echo "done"
# done

SYS_PROPS=""

SYS_PROPS+=" -Djava.library.path='/tmp/dicom-opencv'"

[[ ! -z "$JAVA_OPTS" ]] && SYS_PROPS+=" $JAVA_OPTS"

########################
#  KARNAK ENVIRONMENT  #
########################
file_env 'KARNAK_LOGIN_PASSWORD'
: "${KARNAK_LOGIN_ADMIN:=admin}"
: "${KARNAK_LOGIN_PASSWORD:=undefined}"
SYS_PROPS+=" -Dkarnakadmin='$KARNAK_LOGIN_ADMIN'"
SYS_PROPS+=" -Dkarnakpassword='$KARNAK_LOGIN_PASSWORD'"

# Web portal port inside the container (8081 is the port of the portable package)
: "${KARNAK_WEB_PORT:=8080}"
SYS_PROPS+=" -Dserver.port=$KARNAK_WEB_PORT"

file_env 'DB_ENCRYPTION_KEY'
: "${DB_ENCRYPTION_KEY:=undefined}"
SYS_PROPS+=" -Dkarnakpostgreskey='$DB_ENCRYPTION_KEY'"

##########################
# KARNAK OPENID PROVIDER #
##########################
: "${IDP:=undefined}"
if [[ "$IDP" == "oidc" ]]
then
  file_env 'OIDC_CLIENT_SECRET'
  : "${OIDC_CLIENT_ID:=undefined}"
  : "${OIDC_CLIENT_SECRET:=undefined}"
  : "${OIDC_ISSUER_URI:=undefined}"
  SYS_PROPS+=" -Dspring.security.oauth2.client.registration.keycloak.client-id='$OIDC_CLIENT_ID'"
  SYS_PROPS+=" -Dspring.security.oauth2.client.registration.keycloak.client-secret='$OIDC_CLIENT_SECRET'"
  SYS_PROPS+=" -Dspring.security.oauth2.client.provider.keycloak.issuer-uri='$OIDC_ISSUER_URI'"
fi

########################
# DATABASE ENVIRONMENT #
########################
file_env 'DB_USER'
file_env 'DB_NAME'
file_env 'DB_PASSWORD'
: "${DB_USER:=karnak}"
: "${DB_PASSWORD:=karnak}"
: "${DB_NAME:=karnak}"
: "${DB_HOST:=localhost}"
: "${DB_PORT:=5432}"

DB_URL=jdbc:postgresql://$DB_HOST:$DB_PORT/$DB_NAME

SYS_PROPS+=" -Dspring.datasource.username=$DB_USER"
SYS_PROPS+=" -Dspring.datasource.password=$DB_PASSWORD"
SYS_PROPS+=" -Dspring.datasource.url=$DB_URL"

if [[ -v LOGBACK_CONFIGURATION_FILE ]]
then
  SYS_PROPS+=" -Dlogging.config=$LOGBACK_CONFIGURATION_FILE"
fi

eval java "$SYS_PROPS" -jar application.jar
