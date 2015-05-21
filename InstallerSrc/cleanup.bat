del/S/Q %1\utils
rmdir/S/Q %1\utils
SET STRING=%1
SET STRING=%STRING:"=%
assoc .aoi=aoifile
ftype aoifile="%STRING%\Art of Illusion.exe" %%1
    