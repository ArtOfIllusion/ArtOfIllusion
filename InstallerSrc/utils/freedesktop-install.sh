#gnome installation script
echo "Installing Gnome/KDE Shortcuts..." >> $1/install.log
sh $1/utils/xdg-utils/scripts/xdg-desktop-icon install $1/utils/aoi.desktop --novendor 2>> $1/install.log
sh $1/utils/xdg-utils/scripts/xdg-mime install $1/utils/aoi.xml --novendor 2>> $1/install.log
sh $1/utils/xdg-utils/scripts/xdg-mime default aoi.desktop application/x-aoi 2>> $1/install.log
#sed -e "s%aoi.sh%$1/aoi.sh%" $1/utils/aoi.desktop > $1/utils/tmp 2>> $1/install.log
#sed -e "s%USER_HOME%$2%" $1/utils/tmp > $1/utils/aoi.desktop 2>> $1/install.log
sh $1/utils/xdg-utils/scripts/xdg-desktop-menu install $1/utils/aoi.desktop --novendor 2>> $1/install.log
sh $1/utils/xdg-utils/scripts/xdg-icon-resource install --size 64 $1/utils/icons/64x64/aoi.png --novendor 2>> $1/install.log
sh $1/utils/xdg-utils/scripts/xdg-icon-resource install --size 48 $1/utils/icons/48x48/aoi.png --novendor 2>> $1/install.log
sh $1/utils/xdg-utils/scripts/xdg-icon-resource install --size 32 $1/utils/icons/32x32/aoi.png --novendor 2>> $1/install.log
sh $1/utils/xdg-utils/scripts/xdg-icon-resource install --context mimetypes --size 32 $1/utils/icons/32x32/mime-x-aoi.png application-x-aoi --novendor 2>> $1/install.log
sh $1/utils/xdg-utils/scripts/xdg-icon-resource install --context mimetypes --size 48 $1/utils/icons/48x48/mime-x-aoi.png application-x-aoi --novendor 2>> $1/install.log
sh $1/utils/xdg-utils/scripts/xdg-icon-resource install --context mimetypes --size 64 $1/utils/icons/64x64/mime-x-aoi.png application-x-aoi --novendor 2>> $1/install.log
exit 0
