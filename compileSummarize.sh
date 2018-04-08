set $CP=src
set $CP=$CP:lib/JSON-java
set $CP=$CP:lib/commons-io-2.5/commons-io-2.5.jar
set $CP=$CP:lib/commons-codec-1.10/commons-codec-1.10.jar

set $OUTPUT=bin

set $MAIN=

javac -cp src/:lib/JSON-java:lib/commons-io-2.5/commons-io-2.5.jar:lib/commons-codec-1.10/commons-codec-1.10.jar -d bin/ src/scratchreferee/app/Summarize.java

echo "compile script complete"
