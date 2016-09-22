package sbtdirtymoney

import sbt._
import Keys._

object Plugin extends AutoPlugin {
  override def requires = plugins.JvmPlugin
  override def trigger  = allRequirements

  val cleanCacheIvy2Directory = settingKey[File]("")
  val cleanCacheFiles         = inputKey[Seq[File]]("")
  val cleanCacheFilesPrint    = inputKey[Unit]("")
  val cleanCache              = inputKey[Unit]("")

  val cleanLocalFiles         = inputKey[Seq[File]]("")
  val cleanLocalFilesPrint    = inputKey[Unit]("")
  val cleanLocal              = inputKey[Unit]("")

  object DirtyMoney {
    import complete.Parser
    import complete.DefaultParsers._

    case class ModuleParam(organization: String, name: Option[String])

    def parseParam: Parser[Option[ModuleParam]] = ((parseOrg ~ parseName.?) map { case o ~ n => ModuleParam(o, n) }).?

    private def parseOrg:  Parser[String] = (Space ~> token(StringBasic.examples("\"organization\"")))
    private def parseName: Parser[String] = (Space ~> token(token("%") ~> Space ~> StringBasic.examples("\"name\"")))

    def query(base: File, param: Option[ModuleParam], org: String, name: String): Seq[File] =
      (param match {
        case None                                   => base ** ("*" + org + "*") ** ("*" + name + "*")
        case Some(ModuleParam("*", None))           => base ** "*"
        case Some(ModuleParam(o, None | Some("*"))) => base ** ("*" + o + "*") ** "*"
        case Some(ModuleParam(o, Some(n)))          => base ** ("*" + o + "*") ** ("*" + n + "*")
      }).get
  }

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    cleanCacheIvy2Directory := ivyPaths.value.ivyHome.getOrElse(Path.userHome / ".ivy2"),
    cleanCacheFiles := {
      val base = cleanCacheIvy2Directory.value / "cache"
      val param = DirtyMoney.parseParam.parsed
      DirtyMoney.query(base, param, organization.value, name.value)
    },
    cleanCacheFilesPrint := cleanCacheFiles.evaluated foreach println,
    cleanCache := IO.delete(cleanCacheFiles.evaluated),

    cleanLocalFiles := {
      val base = cleanCacheIvy2Directory.value / "local"
      val param = DirtyMoney.parseParam.parsed
      DirtyMoney.query(base, param, organization.value, name.value)
    },
    cleanLocalFilesPrint := cleanCacheFiles.evaluated foreach println,
    cleanLocal := IO.delete(cleanLocalFiles.evaluated)
  )
}
