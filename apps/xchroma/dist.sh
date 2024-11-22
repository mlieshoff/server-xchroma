#!/bin/bash

source "$HOME/.sdkman/bin/sdkman-init.sh"

sdk use java 21.0.3-oracle

echo create server libs...
mvn clean install
cp target/server-1.0.jar apps/xchroma/libs

echo prepare xchroma release...
cd ../xchroma

echo create jar...
mvn clean install

echo encrypt...
pwd
java -cp ../server-xchroma/apps/xchroma/libs/server-1.0.jar server.FileCipher ../server_credentials/conf/xchroma/key backend/target/backend-1.0.0.jar code ../server-xchroma/apps/xchroma/libs/xchroma-encrypted.jar

echo copy credentials...
cp ../server_credentials/conf/xchroma/encrypted/credentials.properties ../server-xchroma/apps/xchroma/conf