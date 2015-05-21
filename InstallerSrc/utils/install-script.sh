#!/bin/sh

memory=$(cat /proc/meminfo | head -n 1 | awk '{print $2}')
mem=$(($memory/1100))
echo "AoI installation log $(date)" > $1/install.log
#echo "Setting memory to : $mem" >> $1/install.log
#if [ $mem -gt 256 ]; then
#	sed -e "s%-Xmx256m%-Xmx${mem}m%" $1/aoi.sh > $1/aoi2.sh 2>> $1/install.log
#	rm $1/aoi.sh 2>> $1/install.log
#	mv $1/aoi2.sh $1/aoi.sh 2>> $1/install.log
#	chmod +x $1/aoi.sh 2>> $1/install.log	
#fi
sed -e "s%aoi.sh%$1/aoi.sh%" $1/utils/aoi.desktop > $1/utils/aoi2.desktop 2>> $1/install.log
rm $1/utils/aoi.desktop 2>> $1/install.log
mv $1/utils/aoi2.desktop $1/utils/aoi.desktop 2>> $1/install.log
