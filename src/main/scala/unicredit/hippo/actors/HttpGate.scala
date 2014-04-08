package unicredit.hippo
package actors

import scala.concurrent.duration._

import akka.actor.{ Actor, ActorRef }
import akka.util.Timeout
import akka.pattern.ask
import spray.routing.HttpService
import spray.http.HttpMethods._
import spray.httpx.Json4sJacksonSupport
import org.json4s.NoTypeHints
import org.json4s.jackson.JsonMethods._
import org.json4s.JsonDSL._
import org.json4s.jackson.Serialization.formats
import com.typesafe.config.ConfigFactory


import messages.{ Request, Result }


object Support extends Json4sJacksonSupport {
  implicit val json4sJacksonFormats = formats(NoTypeHints)
}

class HttpGate(frontend: ActorRef) extends Actor with HttpService {
  private val config = ConfigFactory.load
  private val tms = config getLong "request.timeout-in-s"
  implicit val timeout = Timeout(tms seconds)

  def actorRefFactory = context
  import context.dispatcher
  import Support._

  def receive = runRoute {
    get {
      parameterMultiMap { params =>
        val message = Request(
          table = params("table").head,
          keys = params("key"),
          columns = params("column")
        )

        complete {
          (frontend ? message).mapTo[Result] map { x => render(x.content) }
        }
      }
    }
  }
}