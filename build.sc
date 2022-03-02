import mill._, scalalib._

trait CommonConfig extendsJavaModule {
  def ivyDeps = Agg(
    ivy
}

object ArtOfIllusion extends JavaModule {
  def ivyDeps = Agg(
    ivy"gov.nist.math::jama:1.0.3"
  , ivy"com.fifesoft::rsyntaxtextarea:3.1.6"
  , ivy"org.apache.groovy::groovy:4.0.0"
  )

 def unmanagedClasspath = Agg(
    mill.modules.Util.download(
      "https://github.com/blackears/svgSalamander/releases/download/v1.1.3/svgSalamander-1.1.3.jar",
      "svgSalamander.jar"
    )
  ,  mill.modules.Util.Download(
    "https://github.com/beanshell/beanshell/releases/download/2.1.0/bsh-2.1.0.jar",
    "bsh.jar"
    )
  , 
  )

}
