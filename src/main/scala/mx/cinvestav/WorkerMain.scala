package mx.cinvestav

import fs2.io.readInputStream
import fs2.Stream
import cats.implicits._
import cats.effect.implicits._
import cats.effect.std.Console
import cats.effect.{Concurrent, ExitCode, IO, IOApp, Sync}
import com.comcast.ip4s.Port
import fs2.Stream.NestedStreamOps
import fs2.io.net.Network
import mx.cinvestav.config.DefaultConfig
import pureconfig._
import pureconfig.generic.auto._
import fs2.{INothing, text}
import io.circe.Decoder
import mx.cinvestav.domain.QueueSimulatorData
import io.circe._
import io.circe.generic.semiauto._
import io.circe.parser.decode

import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date

object WorkerMain extends IOApp{
  implicit val queueSimulatorDataDecoder:Decoder[QueueSimulatorData] = deriveDecoder[QueueSimulatorData]

  def successMessage[F[_]:Console](name:String,port:Int):Stream[F,Unit] =
    Stream.eval(Console[F].println(s"$name running on $port"))

  def ioRuntime[F[_]:Sync]=Sync[F].pure(Runtime.getRuntime)
  def queueSimulatorInputS[F[_]:Sync](avgInterArrival:Float,st:Int,numDelays:Int)(implicit C:DefaultConfig) = {
    val time = Sync[F].pure(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()))
    readInputStream[F](
      ioRuntime[F]
        .map(_.exec(C.queueSim +s" $avgInterArrival $st $numDelays") )
        .map(_.getInputStream)
      ,chunkSize = 4096,closeAfterUse = true)
      .through(text.utf8Decode)
      .filter(_.nonEmpty)
      .evalMap(x=>time.map(y=>(x,y)))
      .debug(x=>s"${x._2} [DEBUG] ${x._1}")
      .fmap(_._1)
  }

  def workerServer[F[_]:Sync:Console:Concurrent:Network]()(implicit C:DefaultConfig): Stream[F, Unit] ={
   successMessage[F](C.name,C.port) ++ Network[F].server(port = Port.fromInt(C.port))
     .map{ client=>
      client.reads
        .through(text.utf8Decode)
        .through(text.lines)
        .filter(x=>x.nonEmpty)
        .map(decode[QueueSimulatorData])
        .map {
          case Left(value) =>
            QueueSimulatorData(1,1,1)
          case Right(value) =>
            value
        }
        .flatMap(x=>queueSimulatorInputS(x.avgInterArrival,x.serviceTime,x.numDelays))
        .through(text.utf8Encode)
        .through(client.writes)
    }
     .parJoin(100)

  }

  override def run(args: List[String]): IO[ExitCode] ={
    val config = ConfigSource.default.load[DefaultConfig]

    config match {
      case Left(value) =>
        println(value)
        IO.unit.as(ExitCode.Error)
      case Right(config) =>
        implicit val _c = config
        workerServer[IO]
          .compile
          .drain
          .as(ExitCode.Success)
    }
  }
//    IO.unit.as(ExitCode.Success)
}
