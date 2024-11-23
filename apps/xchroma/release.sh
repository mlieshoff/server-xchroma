#!/bin/bash

source "$HOME/.sdkman/bin/sdkman-init.sh"

sdk use java 21.0.3-oracle

APP_NAME=xchroma
DIRECTORY=/home/$APP_NAME/server
STATUS=$DIRECTORY/$APP_NAME.pid
APP_DIR=$DIRECTORY/server/apps/$APP_NAME

source "$APP_DIR/server-vars.sh"

cd $DIRECTORY/server

pwd

echo "Refresh from repository...."
git pull
git gc --aggressive --prune

echo "Stop server...."
if [ -f "$STATUS" ]; then
  kill $(cat $STATUS)
  while true; do
    if [ -f "$STATUS" ]; then
      echo "Waiting ..."
    else
      echo "Stopped server."
      break
    fi
  done
fi

cd $APP_DIR/libs

echo download server jar from dropbox...
curl -X POST https://content.dropboxapi.com/2/files/download \
    --header "Authorization: Bearer $DROPBOX_TOKEN" \
    --header "Dropbox-API-Arg: {\"path\":\"/Apps/SERVER_TRANSFER/server-1.0.jar\"}"
echo

echo download $APP_NAME jar from dropbox...
curl -X POST https://content.dropboxapi.com/2/files/download \
    --header "Authorization: Bearer $DROPBOX_TOKEN" \
    --header "Dropbox-API-Arg: {\"path\":\"/Apps/SERVER_TRANSFER/$APP_NAME-encrypted.jar\"}"
echo

cd ..

echo "Decrypt jar..."
/root/.sdkman/candidates/java/21.0.3-oracle/bin/java -Dlog4j2.formatMsgNoLookups=true -cp libs/server-1.0.jar server.FileCipher conf/key libs/$APP_NAME-encrypted.jar decode libs/$APP_NAME.jar

echo "Start server...."
/root/.sdkman/candidates/java/21.0.3-oracle/bin/java -Dlog4j2.formatMsgNoLookups=true -jar libs/$APP_NAME.jar conf/key conf/config.properties conf/credentials.properties $STATUS &

exit 1