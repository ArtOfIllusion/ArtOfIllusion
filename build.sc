import mill._, scalalib._

trait AOIModule extends JavaModule {

  def joglRoot = "https://jogamp.org/deployment/archive/rc/v2.4.0-rc-20210111/jar/"

  def ivyDeps = Agg(
    ivy"gov.nist.math:jama:1.0.3",
    ivy"com.fifesoft:rsyntaxtextarea:3.1.6",
    ivy"org.apache.groovy:groovy:4.0.0"
  )

  def unmanagedClasspath = Agg(
    mill.modules.Util.download(
      "https://github.com/blackears/svgSalamander/releases/download/v1.1.3/svgSalamander-1.1.3.jar",
      os.rel / "svgSalamander.jar"
    ),
    mill.modules.Util.download(
      "https://github.com/beanshell/beanshell/releases/download/2.1.0/bsh-2.1.0.jar",
      os.rel / "bsh.jar"
    ),
    mill.modules.Util.download(joglRoot + "gluegen-rt.jar",
      os.rel / "gluegen-rt.jar"
    ),
    mill.modules.Util.download(joglRoot + "jogl-all.jar",
      os.rel / "jogl-all.jar"
    ),
    PathRef(mill.modules.Util.downloadUnpackZip(
      "https://sourceforge.net/projects/buoy/files/buoy/1.9/Buoy1.9.zip/download",
      os.rel / "buoy").path / "Buoy Folder" / "Buoy.jar"
    ),
    PathRef(mill.modules.Util.downloadUnpackZip(
      "https://sourceforge.net/projects/buoy/files/buoyx/1.9/Buoyx1.9.zip/download",
      os.rel / "buoyx").path / "Buoyx Folder" / "Buoyx.jar"
    ),
    PathRef(millSourcePath / os.up / "lib" / "QuickTimeWriter.jar")
  )
}

object ArtOfIllusion extends AOIModule {
  def mainClass = Some("artofillusion.ArtOfIllusion")
}

object Filters extends AOIModule {
  def moduleDeps = Seq(ArtOfIllusion)
}

object OSSpecific extends AOIModule {
  def moduleDeps = Seq(ArtOfIllusion)
}

object Renderers extends AOIModule {
  def moduleDeps = Seq(ArtOfIllusion)
}

object Tools extends AOIModule {
  def moduleDeps = Seq(ArtOfIllusion)
}

object Translators extends AOIModule {
  def moduleDeps = Seq(ArtOfIllusion)
}