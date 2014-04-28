#!/bin/sh
JAVA_OPTS="{{javaOpts}}"
JAVA_OPTS="$JAVA_OPTS -server"
JAVA_OPTS="$JAVA_OPTS -Djavax.xml.transform.TransformerFactory=org.apache.xalan.xsltc.trax.SmartTransformerFactoryImpl"
JAVA_OPTS="$JAVA_OPTS -Dlumis.portal.lumisDataPath={{lumisDataPath}}"
JAVA_HOME="{{javaHome}}"
export LD_LIBRARY_PATH="$LD_LIBRARY_PATH:$CATALINA_HOME/lib:$CATALINA_HOME/bin/native"
