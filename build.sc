import mill._
import mill.modules.Util.download
import scalalib._

trait Common extends JavaModule {

  def ivyDeps = Agg(
    ivy"gov.nist.math:jama:1.0.3",
    ivy"com.fifesoft:rsyntaxtextarea:3.1.6",
    ivy"org.apache.groovy:groovy:4.0.0"
  )

  def unmanagedClasspath = T {
    Agg(
    PathRef(millSourcePath / os.up / "lib" / "Buoy.jar"),
    PathRef(millSourcePath / os.up / "lib" / "Buoyx.jar"),
    PathRef(millSourcePath / os.up / "lib" / "QuickTimeWriter.jar")
  ) ++ Cache.unmanagedFiles()
  }
}

object ArtOfIllusion extends Common {

  def mainClass = Some("artofillusion.ArtOfIllusion")

  def gitVersion = T.input {
    os.proc("git", "describe", "--first-parent", "--tags", "--always", "--dirty")
      .call().out.text().stripTrailing()
  }

  def libJarPaths = T {
    upstreamAssemblyClasspath()
      .map(_.path.last.prependedAll("lib/"))
      .iterator
      .mkString(" ")
  }

  /**
   * Manifest for ArtOfIllusion requires appropriate Classpath entries
   * and a git version signature
   */
  def manifest: T[mill.modules.Jvm.JarManifest] = T {
    super.manifest()
      .add("Implementation-Version" -> gitVersion.apply())
      .add("Class-Path" -> libJarPaths())
  }

  object test extends Tests with TestModule.Junit4 {
    def runClasspath = super.runClasspath() ++ ArtOfIllusion.upstreamAssemblyClasspath()
  }
}

trait PluginModule extends Common {
  def moduleDeps = Seq(ArtOfIllusion)
}

object Renderers extends PluginModule {
  object test extends Tests with TestModule.Junit4 {
    def runClasspath = super.runClasspath() ++ ArtOfIllusion.upstreamAssemblyClasspath()
  }
}

object Filters extends PluginModule

object OSSpecific extends PluginModule

object Tools extends PluginModule

object Translators extends PluginModule

object Suite extends Module with Common {
  def pluginModules: Seq[PluginModule] = Seq(Filters, OSSpecific, Renderers, Tools, Translators)

  def launch(args: String*) = T.command {
    os.proc("java", args, "-jar", localDeploy().path / "ArtOfIllusion.jar").call()
  }

  def stage = T {
    os.makeDir.all(T.dest / "lib")
    os.makeDir.all(T.dest / "Plugins")
    os.makeDir.all(T.dest / "Textures and Materials")
    os.makeDir.all(T.dest / "Scripts" / "Tools")
    os.makeDir.all(T.dest / "Scripts" / "Startup")
    os.makeDir.all(T.dest / "Scripts" / "Objects")

    ArtOfIllusion.upstreamAssemblyClasspath()
      .map(_.path)
      .filter(p => os.exists(p) && os.isFile(p))
      .iterator
      .foreach(p => os.copy(p, T.dest / "lib" / p.last))

    os.copy(ArtOfIllusion.jar().path, T.dest / "ArtOfIllusion.jar")

    os.copy(Filters.jar().path, T.dest / "Plugins" / Filters.artifactName().concat(".jar"))
    os.copy(OSSpecific.jar().path, T.dest / "Plugins" / OSSpecific.artifactName().concat(".jar"))
    os.copy(Renderers.jar().path, T.dest / "Plugins" / Renderers.artifactName().concat(".jar"))
    os.copy(Tools.jar().path, T.dest / "Plugins" / Tools.artifactName().concat(".jar"))
    os.copy(Translators.jar().path, T.dest / "Plugins" / Translators.artifactName().concat(".jar"))
    mill.modules.Util.download("http://aoisp.sourceforge.net/AoIRepository/Plugins/SPManager/SPManager-3_0.jar",
      os.rel / "Plugins" / "SPManager.jar")
    mill.modules.Util.download("http://aoisp.sourceforge.net/AoIRepository/Plugins/SPManager/PostInstall-3_0.jar",
      os.rel / "Plugins" / "PostInstall.jar")
    PathRef(T.dest)
  }

  def localDeploy = T.persistent {
    os.copy(stage().path, T.dest, replaceExisting = true, mergeFolders = true)
    val osName = System.getProperty("os.name").toLowerCase()
    val natives = osName match {
      case x if osName.contains("linux") => Cache.joglLinuxNatives()
      case x if osName.contains("windows") => Cache.joglWindowsNatives()
      case x if osName.contains("mac") | osName.contains("osx") => Cache.joglMacOSXNatives()
    }

    natives
      .map(_.path)
      .iterator
      .foreach(p => os.copy(p, T.dest / "lib" / p.last, replaceExisting = true))

    PathRef(T.dest)
  }
}

/* Because if we leave this in Common, all the downloaded files will be downloaded/cached
 * per Module object.
 */
object Cache extends Module{
  def unmanagedFiles = T {
    Agg(
      downloadFile(
        "https://github.com/blackears/svgSalamander/releases/download/v1.1.3/svgSalamander-1.1.3.jar"),
      downloadFile(
        "https://github.com/beanshell/beanshell/releases/download/2.1.0/bsh-2.1.0.jar"),
      download("https://maven.jzy3d.org/releases/org/jogamp/gluegen/gluegen-rt/v2.4.0-rc4/gluegen-rt-v2.4.0-rc4.jar",
        os.rel / "gluegen-rt.jar"),
      download("https://maven.jzy3d.org/releases/org/jogamp/jogl/jogl-all/v2.4.0-rc4/jogl-all-v2.4.0-rc4.jar",
        os.rel / "jogl-all.jar")
    )
  }

  def joglLinuxNatives = T {
    Agg(
      download("https://maven.jzy3d.org/releases/org/jogamp/gluegen/gluegen-rt-natives-linux-i586/v2.4.0-rc4/gluegen-rt-natives-linux-i586-v2.4.0-rc4.jar",
        os.rel / "gluegen-rt-natives-linux-i586.jar"),
      download("https://maven.jzy3d.org/releases/org/jogamp/gluegen/gluegen-rt-natives-linux-amd64/v2.4.0-rc4/gluegen-rt-natives-linux-amd64-v2.4.0-rc4.jar",
        os.rel / "gluegen-rt-natives-linux-amd64.jar"),
      download("https://maven.jzy3d.org/releases/org/jogamp/gluegen/gluegen-rt-natives-linux-armv6hf/v2.4.0-rc4/gluegen-rt-natives-linux-armv6hf-v2.4.0-rc4.jar",
        os.rel / "gluegen-rt-natives-linux-armv6hf.jar"),
      download("https://maven.jzy3d.org/releases/org/jogamp/gluegen/gluegen-rt-natives-linux-aarch64/v2.4.0-rc4/gluegen-rt-natives-linux-aarch64-v2.4.0-rc4.jar",
        os.rel / "gluegen-rt-natives-linux-aarch64.jar"),
      download("https://maven.jzy3d.org/releases/org/jogamp/jogl/jogl-all-natives-linux-i586/v2.4.0-rc4/jogl-all-natives-linux-i586-v2.4.0-rc4.jar",
        os.rel / "jogl-all-natives-linux-i586-v2.4.0-rc4.jar"),
      download("https://maven.jzy3d.org/releases/org/jogamp/jogl/jogl-all-natives-linux-amd64/v2.4.0-rc4/jogl-all-natives-linux-amd64-v2.4.0-rc4.jar",
        os.rel / "jogl-all-natives-linux-amd64.jar"),
      download("https://maven.jzy3d.org/releases/org/jogamp/jogl/jogl-all-natives-linux-armv6hf/v2.4.0-rc4/jogl-all-natives-linux-armv6hf-v2.4.0-rc4.jar",
        os.rel / "jogl-all-natives-linux-armv6hf.jar"),
      download("https://maven.jzy3d.org/releases/org/jogamp/jogl/jogl-all-natives-linux-aarch64/v2.4.0-rc4/jogl-all-natives-linux-aarch64-v2.4.0-rc4.jar",
        os.rel / "jogl-all-natives-linux-aarch64-v2.4.0-rc4.jar")
    )
  }

  def joglWindowsNatives = T {
    Agg(
      download("https://maven.jzy3d.org/releases/org/jogamp/gluegen/gluegen-rt-natives-windows-i586/v2.4.0-rc4/gluegen-rt-natives-windows-i586-v2.4.0-rc4.jar",
        os.rel / "gluegen-rt-natives-windows-i586.jar"),
      download("https://maven.jzy3d.org/releases/org/jogamp/gluegen/gluegen-rt-natives-windows-amd64/v2.4.0-rc4/gluegen-rt-natives-windows-amd64-v2.4.0-rc4.jar",
        os.rel / "gluegen-rt-natives-windows-amd64.jar"),
      download("https://maven.jzy3d.org/releases/org/jogamp/jogl/jogl-all-natives-windows-i586/v2.4.0-rc4/jogl-all-natives-windows-i586-v2.4.0-rc4.jar",
        os.rel / "jogl-all-natives-windows-i586-v2.4.0-rc4.jar"),
      download("https://maven.jzy3d.org/releases/org/jogamp/jogl/jogl-all-natives-windows-amd64/v2.4.0-rc4/jogl-all-natives-windows-amd64-v2.4.0-rc4.jar",
        os.rel / "jogl-all-natives-windows-amd64-v2.4.0-rc4.jar")
    )
  }

  def joglMacOSXNatives = T {
    Agg(
      download("https://maven.jzy3d.org/releases/org/jogamp/gluegen/gluegen-rt-natives-macosx-universal/v2.4.0-rc4/gluegen-rt-natives-macosx-universal-v2.4.0-rc4.jar",
        os.rel / "gluegen-rt-natives-macosx-universal.jar"),
      download("https://maven.jzy3d.org/releases/org/jogamp/jogl/jogl-all-natives-macosx-universal/v2.4.0-rc4/jogl-all-natives-macosx-universal-v2.4.0-rc4.jar",
        os.rel / "jogl-all-natives-macosx-universal-v2.4.0-rc4.jar")
    )
  }

  def downloadFile(url: String)(implicit ctx: mill.util.Ctx.Dest) = {
    val fileName = url.split("/".charAt(0)).last
    mill.modules.Util.download(url, os.rel / fileName)
  }
}
