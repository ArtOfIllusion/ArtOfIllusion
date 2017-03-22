# Building AOI from source

If you've never built AOI from source before, please follow the process
below

## Prerequisites

Here's a list of the tools that you will need to work on the AOI
source code: (If you are an experienced java developer, you probably
have all of these already!)

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
 * [Git,](https://git-scm.com/downloads) for tracking changes that
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

To build AOI, type: `ant -f ArtOfIllusion.xml`. This will create a working
AOI build, suitable for testing and experimentation.

To launch AOI, Double-click the `ArtOfIllusion.jar` file. You may also
launch from the command line with `java -jar ArtOfIllusion.jar`.
Launching from the command line will allow you to see any console output
which AOI generates. You can also add command line options to allow AOI
to use more memory, etc.

### Enabling OpenGL

AOI can use your graphics card to make rendering of scenes in the
editor windows faster. To make this work in the development build,
you need to move some files around. Copy all .jar files from
`InstallerSrc/jogl-<your platform>` to `lib`. Remember to restart
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

If you've followed the above instructions, your working directory will
contain the following directories and files:

 * _ArtOfIllusion_ (source for the main application)
 * _InstallerSrc_ (Files for creating the automated installers)
 * __*lib*__ (Binary dependencies in jar form)
 * _OSSpecific_ (source for a plugin that improves integration
into mac environments)
 * __Plugins__ (Instalation directory where AOI looks for installed
plugins. These include the standard modeling tools and file
translators)
 * _Renderers_ (source for the standard rendering package plugin)
 * __Scripts__ (Installation directory where beanshell and groovy
scripts live)
 * _Tests_ (source for automated JUnit tests. Not present in AOI when
installed using the installer packages
 * __Textures and Materials__ (Installation directory that holds a library
of .aoi files that contain re-usable textures and materials)
 * _Tools_ (source for the standard modeling tools plugin)
 * _Translators_ (source for the standard file importer/exporter plugin)
 * __ArtOfIllusion.jar__ (the compiled main application)
 * _ArtOfIllusion.xml_, _OSSpecific.xml_, _Renderers.xml_, _Tools.xml_
and _Translators.xml_ (ant build files for the various parts of the
package.)

The files and directories marked by _italics_ have contents that are
managed by the version control sytem. This includes all of the source
code files that make up AOI, and some smaller libraries that we depend
on. The directories in __bold__ are required for AOI to run properly.
They are generally created by the build process. The plugins directory
is populated by the standard plugins, and these are overwritten when
you rebuild. Any other plugins in this directory are not touched, and
can continue to be used from build to build.

### Building installers
Right now, you can't really build a native installer for AOI yourself.
The installer system depends on an old version of IZPack that is not
supported, and that you cannot get an installer for. Some of the
packaging scripts use absolute paths that are unique to one specific
computer. This is an area that we intend to improve soon.
