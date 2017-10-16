# Building AOI from source

If you are an experienced developer, just run `ant` in the main
directory. Build artifacts are located in `Live_Application`.

If you're new to building java programs, please follow the detailed
process and explanation below:

## Prerequisites

Here's a list of the tools that you will need to work on the AOI
source code:

 * A Java Developer Kit, java 6 or higher. This is a compiler that
turns the java text files into something that the Java runtime can
understand. You can download
[the current version from Oracle,](http://www.oracle.com/technetwork/java/javase/downloads/index.html) 
or if you are a Linux user, you can probably get an up to date version
of OpenJDK (an open-source implementation of the tool) through your
package manager.
 * [Apache ant version 1.9.x or compatible.](https://ant.apache.org/bindownload.cgi)
A simple tool to automate compiling and packaging a large number of
java files. Again, Linux users can probably get this through their
package manager.
 * [Git.](https://git-scm.com/downloads) for tracking changes that
you have made to the code, and coordinating with other people.
 * An editor suitable for use with program source code. This means
__something other than notepad or Microsoft Word.__ One good, free
option for Windows users is [notepad++.](https://notepad-plus-plus.org)
Alternatively, you may use an Integrated Development Environment,
such as eclipse or Netbeans.
 * [JUnit 4.x](http://junit.org/junit4/) (optional) framework for
running the automated test suite.

## Obtaining and building the sources

Create a new directory on your hard drive where you would like to
keep your AOI development build, then access it from a command line:

 * If you have a github account, with an SSH key, type:
`git clone git@github.com:ArtOfIllusion/AtrOfIllusion.git`
 * If you don't have an account, or don't use SSH, type:
`git clone https://github.com/ArtOfIllusion/ArtOfIllusion.git`

This will access the repository on github and make a copy in your
directory. This may take a few minutes.

To build AOI, type: `ant`. This will create a working AOI application
in the subdirectory `Live_Application`. This application is suitable
for testing and experimentation, but will not have desktop integration
and filetype association.

To launch AOI, Double-click the `ArtOfIllusion.jar` file in your
installation directory. You may also
launch from the command line with `java -jar ArtOfIllusion.jar`.
Launching from the command line will allow you to see any console output
which AOI generates. You can also add command line options to allow AOI
to use more memory, etc.

### Enabling OpenGL

AOI can use your graphics card to make rendering of scenes in the
editor windows faster. To make this work in the development build,
you need to move some files around. Copy all .jar files from
`InstallerSrc/jogl-<your platform>` to `Live_Application/lib`. Remember to restart
AOI after doing this.

### Installing Plugins

A standard Art of Illusion development build does not include the
SPManager, which is developed as a separate project. You
can copy `SPManager.jar` and `PostInstall.jar` from the plugins
directory of a normal AOI installation, or you can [download them
directly](https://aoisp.sourceforge.net/AoIRepository/Plugins/SPManager)
Just place them in your `Plugins` directory, and restart AOI. You
should now be able to install plugins normally.

If you choose to install plugins this way, you should only need to
install them once. They will not be deleted when you re-build AOI.


### Structure of a development build

If you've followed the above instructions, your `Live_Application` directory will
contain the following directories and files:

 * __Plugins__ Instalation directory where AOI looks for installed
plugins. These include the standard modeling tools and file
translators. The standard plugins are overwritten when
you rebuild. Any other plugins in this directory are not touched, and
can continue to be used from build to build.
 * __Scripts__ Installation directory where beanshell and groovy
scripts live
 * __Textures and Materials__ Installation directory that holds a library
of .aoi files that contain re-usable textures and materials
 * __ArtOfIllusion.jar__ the compiled main application


If you built AOI sometime before 09/19/2017, you probably have copies
the above items in your main source directory. It is safe to delete
them from the source tree. (These old copies will _not_ be updated by
new rebuilds!)


### Building installers
AOI installers for Linux and Windows are built using IZPack. The
templates are found in `InstallerSrc` Apple OS mountable .dmg files
are built by hand.

Building any of these yourself should not be neccessary, and is not
recommended.
