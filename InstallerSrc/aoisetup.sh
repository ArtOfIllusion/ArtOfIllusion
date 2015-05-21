#!/bin/sh 

#Best guess for AoI path 
if [ $0 = "aoi.sh" ]; then
	AOICMD=$(which aoisetup.sh)
	if [ ! -e "$AOICMD" ]; then
		#workaround "sh aoisetup.sh"
		AOIPATH="."
	else
		AOIPATH="${AOICMD%/*}"
	fi
else
	#not in path
	AOIPATH="${0%/*}"
fi

#java command
#first, let's try which
JAVACMD=$(which java)
if [ ! -e "$JAVACMD" ]; then
	# JAVA_HOME set?
	if [ "" != "$JAVA_HOME" ]; then
		JAVACMD="$JAVA_HOME/bin/java"
	else
		#let's try the /usr/java
		javalist=$(find /usr/java/*/bin/java )
		for java in $javalist; do
			if [ "" = "$JAVACMD" ]; then
				JAVACMD=$java
			elif [ "$java" -nt "$JAVACMD" ]; then
				JAVACMD=$java
			fi
		done
	fi
fi

if [ -z "$JAVACMD" ]; then
	echo "Java VM not found. Please use -java=/path/to/java to specify the java command location"
	exit 1	
fi 

JAVACMD="$JAVACMD -jar aoi-linux-install.jar"
echo $JAVACMD
$JAVACMD
