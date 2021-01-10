#gnome uninstallation script
echo "Uninstalling Gnome/KDE Shortcuts..." >> $1/uninstall.log
sh $1/utils/xdg-utils/scripts/xdg-desktop-icon uninstall $1/utils/aoi.desktop 2>> $1/uninstall.log
sh $1/utils/xdg-utils/scripts/xdg-mime uninstall $1/utils/aoi.xml 2>> $1/uninstall.log
sh $1/utils/xdg-utils/scripts/xdg-desktop-menu uninstall $1/utils/aoi.desktop 2>> $1/uninstall.log
sh $1/utils/xdg-utils/scripts/xdg-icon-resource uninstall --size 64 $1/utils/icons/64x64/aoi.png 2>> $1/uninstall.log
sh $1/utils/xdg-utils/scripts/xdg-icon-resource uninstall --size 48 $1/utils/icons/48x48/aoi.png 2>> $1/uninstall.log
sh $1/utils/xdg-utils/scripts/xdg-icon-resource uninstall --size 32 $1/utils/icons/32x32/aoi.png 2>> $1/uninstall.log
sh $1/utils/xdg-utils/scripts/xdg-icon-resource uninstall --context mimetypes --size 32 $1/utils/icons/32x32/mime-x-aoi.png application-x-aoi 2>> $1/uninstall.log
sh $1/utils/xdg-utils/scripts/xdg-icon-resource uninstall --context mimetypes --size 48 $1/utils/icons/48x48/mime-x-aoi.png application-x-aoi 2>> $1/uninstall.log
sh $1/utils/xdg-utils/scripts/xdg-icon-resource uninstall --context mimetypes --size 64 $1/utils/icons/64x64/mime-x-aoi.png application-x-aoi 2>> $1/uninstall.log
exit 0
