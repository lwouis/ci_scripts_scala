import monix.eval.Task
import org.rogach.scallop._

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

object RunOnFiles {
  def main(args: Array[String]) {
    import monix.execution.Scheduler.Implicits.global
    val conf = new Conf(args)

    Task {
      var std = ""
      val logger = ProcessLogger(s => std = s)
      val value = s"${System.getProperty("user.dir")}/toto/random_process.sh $a"
      val p = Process(Seq("sh", "-c", value)).run(logger)
      Seq(a, p.exitValue(), std)
    })

    val value = Task.gather(
      Range(0, 21).map(a => Task {
        var std = ""
        val logger = ProcessLogger(s => std = s)
        val value = s"${System.getProperty("user.dir")}/toto/random_process.sh $a"
        val p = Process(Seq("sh", "-c", value)).run(logger)
        Seq(a, p.exitValue(), std)
      })
    )
    val async = value.runAsync
    async.foreach(b => println(b))

    Await.result(async, Duration.Inf)
  }
}
