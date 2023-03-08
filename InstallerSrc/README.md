Buillding Installers
~~~~~~~~~~~~~~~~~~~~



Windows 
#######


Quirks:
=======
The native launchers for Windows are built using a patched version of launch4j.

Many newer JRE's don't list themselves in the windows registry, or use
non-standard keys. The launcher needs to be patched to search for the 
`%JAVA_HOME%` environment variable, or failing that, scan the entire `%PATH%`.

See https://sourceforge.net/p/launch4j/feature-requests/127/ for more detail,
and a copy of the `head.o` file that needs to be patched in for the launchers
to work correctly. Launch4j built from source after 2019-01-31, or greater than
version 3.5 already have this patch included.
