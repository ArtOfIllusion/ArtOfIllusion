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
    ivy"org.jogamp.gluegen:gluegen-rt:v2.4.0-rc-20210111",
    ivy"org.jogamp.jogl:jogl-all:v2.4.0-rc-20210111",
    ivy"gov.nist.math:jama:1.0.3",
    ivy"com.fifesoft:rsyntaxtextarea:3.1.6",
    ivy"org.apache.groovy:groovy:4.0.0"
  )

  def nativeDeps(platform: String) = T.task {
    Agg(
      ivy"org.jogamp.jogl:jogl-all-natives-${platform}:v2.4.0-rc-20210111",
      ivy"org.jogamp.gluegen:gluegen-rt-natives-${platform}:v2.4.0-rc-20210111"
    )
  }

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
    os.proc("java", "-jar", localDeploy().path / "ArtOfIllusion.jar").call(cwd = localDeploy().path)
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
    resolveDeps(nativeDeps(System.getProperty("os.name").toLowerCase() ++ "-" ++ System.getProperty("os.arch")))
      .apply()
      .iterator
      .foreach(p => os.copy.into(p.path, T.dest / "lib", replaceExisting = true, mergeFolders = true))
    PathRef(T.dest)
  }
}
