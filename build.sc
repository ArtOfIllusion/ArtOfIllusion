import mill._
import scalalib._
import coursier.maven.MavenRepository

trait AOIModule extends JavaModule {

  def repositoriesTask = T.task { super.repositoriesTask() ++ Seq(
    // Official JOGL project has not put the 2.4.0 RC on maven.
    // This is an alternate location
    MavenRepository("https://maven.jzy3d.org/releases/")
  ) }

  def ivyDeps = Agg(
    ivy"org.jogamp.gluegen:gluegen-rt:v2.4.0-rc4",
    ivy"org.jogamp.jogl:jogl-all:v2.4.0-rc4",
    ivy"gov.nist.math:jama:1.0.3",
    ivy"com.fifesoft:rsyntaxtextarea:3.1.6",
    ivy"org.apache.groovy:groovy:4.0.0"
  )

  def unmanagedClasspath = Agg(
    PathRef(millSourcePath / os.up / "lib" / "Buoy.jar"),
    PathRef(millSourcePath / os.up / "lib" / "Buoyx.jar"),
    PathRef(millSourcePath / os.up / "lib" / "QuickTimeWriter.jar")
  ) ++ Common.unmanagedDownloads.apply()
}

object Common extends Module {
  def unmanagedDownloads = T {
    Agg(
      mill.modules.Util.download(
        "https://github.com/blackears/svgSalamander/releases/download/v1.1.3/svgSalamander-1.1.3.jar",
        os.rel / "svgSalamander.jar"
      ),
      mill.modules.Util.download(
        "https://github.com/beanshell/beanshell/releases/download/2.1.0/bsh-2.1.0.jar",
        os.rel / "bsh.jar"
      )
    )
  }
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