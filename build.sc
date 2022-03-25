import mill._
import scalalib._
import coursier.maven.MavenRepository

trait AOIModule extends JavaModule {

  def ivyDeps = Agg(
    ivy"gov.nist.math:jama:1.0.3",
    ivy"com.fifesoft:rsyntaxtextarea:3.1.6",
    ivy"org.apache.groovy:groovy:4.0.0"
  )

  def unmanagedClasspath = Agg(
    PathRef(millSourcePath / os.up / "lib" / "Buoy.jar"),
    PathRef(millSourcePath / os.up / "lib" / "Buoyx.jar"),
    PathRef(millSourcePath / os.up / "lib" / "QuickTimeWriter.jar")
  ) ++ Common.unmanagedJavaDownloads.apply()
}

object Common extends Module {
  def unmanagedJavaDownloads = T {
    Agg(
      downloadFile(
        "https://github.com/blackears/svgSalamander/releases/download/v1.1.3/svgSalamander-1.1.3.jar"),
      downloadFile(
        "https://github.com/beanshell/beanshell/releases/download/2.1.0/bsh-2.1.0.jar"),
      downloadFile(
        "https://jogamp.org/deployment/archive/rc/v2.4.0-rc-20210111/jar/gluegen-rt.jar"),
      downloadFile(
        "https://jogamp.org/deployment/archive/rc/v2.4.0-rc-20210111/jar/jogl-all.jar")
    )
  }

  def joglLinuxNatives = T {
    Agg(
      downloadFile(
        "https://jogamp.org/deployment/archive/rc/v2.4.0-rc-20210111/jar/gluegen-rt-natives-linux-i586.jar"),
      downloadFile(
        "https://jogamp.org/deployment/archive/rc/v2.4.0-rc-20210111/jar/gluegen-rt-natives-linux-amd64.jar"),
      downloadFile(
        "https://jogamp.org/deployment/archive/rc/v2.4.0-rc-20210111/jar/gluegen-rt-natives-linux-armv6hf.jar"),
      downloadFile(
        "https://jogamp.org/deployment/archive/rc/v2.4.0-rc-20210111/jar/gluegen-rt-natives-linux-aarch64.jar"),
      downloadFile(
        "https://jogamp.org/deployment/archive/rc/v2.4.0-rc-20210111/jar/jogl-all-natives-linux-i586.jar"),
      downloadFile(
        "https://jogamp.org/deployment/archive/rc/v2.4.0-rc-20210111/jar/jogl-all-natives-linux-amd64.jar"),
      downloadFile(
        "https://jogamp.org/deployment/archive/rc/v2.4.0-rc-20210111/jar/jogl-all-natives-linux-armv6hf.jar"),
      downloadFile(
        "https://jogamp.org/deployment/archive/rc/v2.4.0-rc-20210111/jar/jogl-all-natives-linux-aarch64.jar")
    )
  }
    def joglWindowsNatives = T {
      Agg(
        downloadFile(
          "https://jogamp.org/deployment/archive/rc/v2.4.0-rc-20210111/jar/gluegen-rt-natives-windows-i586.jar"),
        downloadFile(
          "https://jogamp.org/deployment/archive/rc/v2.4.0-rc-20210111/jar/gluegen-rt-natives-windows-amd64.jar"),
        downloadFile(
          "https://jogamp.org/deployment/archive/rc/v2.4.0-rc-20210111/jar/jogl-all-natives-windows-i586.jar"),
        downloadFile(
          "https://jogamp.org/deployment/archive/rc/v2.4.0-rc-20210111/jar/jogl-all-natives-windows-amd64.jar")
      )
    }

  def joglMacOSXNatives = T {
    Agg(
      downloadFile(
      "https://jogamp.org/deployment/archive/rc/v2.4.0-rc-20210111/jar/gluegen-rt-natives-macosx-universal.jar"),
      downloadFile(
        "https://jogamp.org/deployment/archive/rc/v2.4.0-rc-20210111/jar/jogl-all-natives-macosx-universal.jar")
    )
  }

  def downloadFile(url: String)(implicit ctx: mill.util.Ctx.Dest)= {
    val fileName = url.split("/".charAt(0)).last
    mill.modules.Util.download(url, os.rel / fileName)
  }
}

object ArtOfIllusion extends AOIModule {
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

object Suite extends AOIModule {

  def launch() = T.command {
    os.proc("java", "-jar", localDeploy().path / "ArtOfIllusion.jar").call()
  }

  def stage = T {
    os.makeDir.all(T.dest / "lib")
    os.makeDir.all(T.dest / "Plugins")
    os.makeDir.all(T.dest / "Textures and Materials")
    os.makeDir.all(T.dest / "Scripts" / "Tools")
    os.makeDir.all(T.dest / "Scripts" / "Startup")
    os.makeDir.all(T.dest / "Scripts" / "Objects")

    upstreamAssemblyClasspath()
      .map(_.path)
      .filter(p => os.exists(p) && os.isFile(p))
      .iterator
      .foreach(p => os.copy(p, T.dest / "lib" / p.last ))

    os.copy(ArtOfIllusion.jar().path, T.dest / "ArtOfIllusion.jar")

    os.copy((Filters.jar().path), T.dest / "Plugins" / Filters.artifactName().concat(".jar"))
    os.copy((OSSpecific.jar().path), T.dest / "Plugins" / OSSpecific.artifactName().concat(".jar"))
    os.copy((Renderers.jar().path), T.dest / "Plugins" / Renderers.artifactName().concat(".jar"))
    os.copy((Tools.jar().path), T.dest / "Plugins" / Tools.artifactName().concat(".jar"))
    os.copy((Translators.jar().path), T.dest / "Plugins" / Translators.artifactName().concat(".jar"))
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
      case x if osName.contains("linux") => Common.joglLinuxNatives.apply()
      case x if osName.contains(("windows")) => Common.joglWindowsNatives.apply()
      case x if osName.contains("mac") | osName.contains("osx") => Common.joglMacOSXNatives.apply()
     }

    natives
      .map(_.path)
      .iterator
      .foreach(p => os.copy(p, T.dest / "lib" / p.last, replaceExisting = true))

    PathRef(T.dest)
  }
}
