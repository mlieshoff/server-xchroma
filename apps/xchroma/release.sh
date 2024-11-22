#!/bin/bash

source "$HOME/.sdkman/bin/sdkman-init.sh"

sdk use java 21.0.3-oracle

DIRECTORY=/home/xchroma/server
STATUS=$DIRECTORY/xchroma.pid
APP=$DIRECTORY/server/apps/xchroma

cd $DIRECTORY/server

pwd

echo "Refresh from repositiory...."
git pull

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

cd $APP

echo "Decrypt jar..."
/root/.sdkman/candidates/java/21.0.3-oracle/bin/java -Dlog4j2.formatMsgNoLookups=true -cp libs/server-1.0.jar server.FileCipher conf/key libs/xchroma-encrypted.jar decode libs/xchroma.jar

echo "Start server...."
/root/.sdkman/candidates/java/21.0.3-oracle/bin/java -Dlog4j2.formatMsgNoLookups=true -jar libs/xchroma.jar conf/key conf/config.properties conf/credentials.properties $STATUS &

exit 1