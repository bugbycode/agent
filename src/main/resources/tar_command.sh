#!/bin/bash
tmp=$1
basepath=$(cd `dirname $0`; pwd)
mkdir -p ${basepath}/${tmp}
cp -rf agent ${basepath}/${tmp}/
cd ${basepath}/${tmp}
echo "server.port=8088" > agent/config/application.properties
echo "spring.netty.clientId=${2}" >> agent/config/application.properties
echo "spring.netty.secret=${3}" >> agent/config/application.properties
echo "spring.oauth.oauthUri=https://192.168.1.206/agent-oauth2/oauth/token" >> agent/config/application.properties
echo "console.uri=https://192.168.1.206/proxy-console/api/getConnHost" >> agent/config/application.properties
echo "server.keystorePath=config/localhost.keystore" >> agent/config/application.properties
echo "server.keystorePassword=changeit" >> agent/config/application.properties
echo "logging.config=classpath:log4j2.xml" >> agent/config/application.properties
echo "spring.thymeleaf.cache=false" >> agent/config/application.properties
echo "spring.thymeleaf.mode=LEGACYHTML5" >> agent/config/application.properties

tar -cvf ${tmp}.tar * --remove-files
