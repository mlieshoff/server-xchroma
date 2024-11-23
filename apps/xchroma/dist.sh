#!/bin/bash

APP_NAME=xchroma

source "$HOME/.sdkman/bin/sdkman-init.sh"
source "../server_credentials/conf/server-vars.sh"

sdk use java 21.0.3-oracle

echo create server libs...
mvn clean install

echo prepare $APP_NAME release...
cd ../$APP_NAME

echo create jar...
mvn clean install

echo encrypt...
pwd
java -cp ../server-$APP_NAME/target/server-1.0.jar server.FileCipher ../server_credentials/conf/$APP_NAME/key backend/target/backend-1.0.0.jar code ../server-$APP_NAME/target/$APP_NAME-encrypted.jar

echo copy credentials...
cp ../server_credentials/conf/$APP_NAME/encrypted/credentials.properties ../server-$APP_NAME/apps/$APP_NAME/conf

echo upload server jar to dropbox...
curl -X POST https://content.dropboxapi.com/2/files/upload \
    --header "Authorization: Bearer $DROPBOX_TOKEN" \
    --header "Dropbox-API-Arg: {\"autorename\":false,\"mode\":\"overwrite\",\"mute\":false,\"path\":\"/Apps/SERVER_TRANSFER/server-1.0.jar\",\"strict_conflict\":false}" \
    --header "Content-Type: application/octet-stream" \
    --data-binary @../server-$APP_NAME/target/server-1.0.jar

echo
echo upload $APP_NAME jar to dropbox...
curl -X POST https://content.dropboxapi.com/2/files/upload \
    --header "Authorization: Bearer $DROPBOX_TOKEN" \
    --header "Dropbox-API-Arg: {\"autorename\":false,\"mode\":\"overwrite\",\"mute\":false,\"path\":\"/Apps/SERVER_TRANSFER/$APP_NAME-encrypted.jar\",\"strict_conflict\":false}" \
    --header "Content-Type: application/octet-stream" \
    --data-binary @../server-$APP_NAME/target/$APP_NAME-encrypted.jar

echo
echo done.