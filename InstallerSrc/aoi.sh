#!/bin/sh 

#NetBSD workaround
os=$(uname)
if [ "$os" = "NetBSD" ]; then
	ulimit -d 656020
fi

#Best guess for AoI path 
if [ $0 = "aoi.sh" ]; then
	AOICMD=$(which aoi.sh)
	if [ ! -e "$AOICMD" ]; then
		#workaround "sh aoi.sh"
		AOIPATH="."
	else
		AOIPATH="${AOICMD%/*}"
	fi
else
	#not in path
	AOIPATH="${0%/*}"
fi

#set library path
export LD_LIBRARY_PATH="$LD_LIBRAY_PATH:$AOIPATH:$AOIPATH/lib"

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

#java command option
for option in "$@"; do
	if [ -z ${option%-java=*} ]; then
		JAVACMD=${option#-java=*}
	fi
done

if [ -z "$JAVACMD" ]; then
	echo "Java VM not found. Please use -java=/path/to/java to specify the java command location"
	exit 1	
fi 

#default mem
MEM=-Xmx1500m

#java command line options
for option in "$@"; do
	if [ -z ${option%-*} ] && [ "" != "${option%-java=*}" ]; then
		JAVACMD="$JAVACMD $option"
	fi
	if [ -z ${option%-Xmx*} ]; then
		MEM=""
	fi
done
if [ -e $AOIPATH/lib/jogl.jar ]; then
	JOGL="$AOIPATH/jogl.jar"
fi
if [ -e $AOIPATH/lib/gluegen-rt.jar ]; then
	JOGL="$AOIPATH/gluegen-rt.jar"
fi
if [ -e $AOIPATH/lib/jmf.jar ]; then
	JMF="$AOIPATH/jmf.jar"
fi
JAVACMD="$JAVACMD $MEM -cp $AOIPATH/ArtOfIllusion.jar:$JOGL:$JMF artofillusion.ArtOfIllusion"

#AoI command line options
for option in "$@"; do
	if [ "$option" = "${option%-*}" ]; then
		JAVACMD="$JAVACMD $option"
	fi
done
echo $JAVACMD
$JAVACMD
