import cats.effect.IO
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.funsuite.AnyFunSuite
import mx.cinvestav.WorkerMain.queueSimulatorInputS
import mx.cinvestav.config.DefaultConfig
import cats.implicits._
import cats.effect.unsafe.implicits.global
import fs2.text

import java.text.SimpleDateFormat
import java.util.Date

class WorkerSpec extends AnyFunSuite{
  test(""){
    implicit val config = DefaultConfig(6060,"localhost","worker",
      "/home/nacho/Programming/C/singleServer_traceGenerator2/QueueSimulator/single")
    val time = IO(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()))
    val queueSimData = queueSimulatorInputS[IO](10,1,200)
      queueSimData
//        .through(text.utf8Decode)
//        .evalMap(x=>time.map(y=>(x,y)))
//        .debug(x=>s"${x._2} [DEBUG] ${x._1}")
        .compile
        .drain
        .unsafeRunSync()
  }

}
