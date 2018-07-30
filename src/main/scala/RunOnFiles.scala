import better.files._
import monix.eval.Task
import org.rogach.scallop._
import scala.compat.Platform.EOL

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.sys.process.{Process, ProcessLogger}

class Conf(arguments: Seq[String]) extends ScallopConf(arguments) {
  val process = opt[String](required = true, descr = "process to run on each file")
  val extensions = opt[String](descr = "file extensions whitelist")
  val ignore = opt[String](descr = "file path blacklist")
  val limit = opt[Int](descr = "how many process are spawn in parallel", default = Some(Runtime.getRuntime.availableProcessors))
  val full = toggle(descrYes = "run on all project files (default is to run on files modified from target git branch)")
  val logOnSuccess = toggle(descrYes = "log process output even when it returned code 0")
  val silent = toggle(descrYes = "disable all logging")
  verify()
}

case class ProcessOutput(name: String, exitValue: Int, stdout: String)

object RunOnFiles {
  def processTask(shellCommand: String*): Task[ProcessOutput] = {
    Task {
      var std = ""
      val p = Process(Seq("sh", "-c", shellCommand.mkString(" ")))
        .run(ProcessLogger(s => std = s))
      ProcessOutput(shellCommand.mkString(" "), p.exitValue, std)
    }
  }

  def main(args: Array[String]) {
    import monix.execution.Scheduler.Implicits.global
    val conf = new Conf(args)

//    val set = allFiles
//      .map(Seq(conf.process(), _))
//      .map(processTask)

    val async = processTask("git", "ls-files")
      .flatMap(a => {
        val strings = a.stdout.split(EOL).toSeq
        val toto = strings.map(b => processTask(b))
        val value = Task.gather(toto)
        value
      })
      .runAsync
        .map(a => {
          println(a)
          a
        })

    Await.result(async, Duration.Inf)
  }

  private def allFiles: Seq[String] = {
    ("." / "toto" / "src")
      .listRecursively()
      .toSeq
      .filter(_.isRegularFile)
      .map(_.path.toAbsolutePath.toString)
      .distinct
      .sorted
  }
}
