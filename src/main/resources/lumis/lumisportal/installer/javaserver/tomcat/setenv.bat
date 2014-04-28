@echo off
set JAVA_OPTS={{javaOpts}}
set JAVA_OPTS=%JAVA_OPTS% -server
set JAVA_OPTS=%JAVA_OPTS% -Djavax.xml.transform.TransformerFactory=org.apache.xalan.xsltc.trax.SmartTransformerFactoryImpl
set JAVA_OPTS=%JAVA_OPTS% -Dlumis.portal.lumisDataPath="{{lumisDataPath}}"
set JAVA_HOME={{javaHome}}
set PATH=%PATH%;%CATALINA_HOME%\lib;%CATALINA_HOME%\bin\native
