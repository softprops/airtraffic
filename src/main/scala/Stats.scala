package airtraffic

import java.io.{ File, InputStreamReader, PrintWriter }
import java.nio.CharBuffer
import java.nio.channels.Channels
import jnr.unixsocket.{ UnixSocketAddress, UnixSocketChannel }


object Stats {
  sealed trait Statable {
    def typ: Int
    def and(that: Statable) =
      new Statable.Value(this.typ + that.typ)
  }
  object Statable {
    class Value(val typ: Int) extends Statable
    case object Frontends extends Value(1)
    case object Backends extends Value(2)
    case object Servers extends Value(4)
    case object Any extends Value(-1)
  }

  sealed trait Proxy {
    def id: String
  }
  object Proxy {
    abstract class Value(val id: String) extends Proxy
    case object Any extends Value("-1")
    case class Id(id: String) extends Proxy
  }

  sealed trait Server {
    def id: String
  }
  object Server {
    abstract class Value(val id: String) extends Server
    case object Any extends Value("-1")
    case class Id(id: String) extends Server
  }

  sealed trait Fallible {
    def id: String
  }
  object Fallible {
    abstract class Value(val id: String) extends Fallible
    case object Any extends Value("")
    case class Id(id: String) extends Fallible
  }
}

/** see section 9.2 (  Unix Socket commands ) of http://haproxy.1wt.eu/download/1.5/doc/configuration.txt
 *  @param path is a file to the unix domain socket defined on your haproxy configs `stats socket <path>` */
case class Stats(path: File) {
  import airtraffic.Stats._

  def request(command: String): String = {
    println(s"req $command")
    val chan = UnixSocketChannel.open(new UnixSocketAddress(path))
    val writer = new PrintWriter(Channels.newOutputStream(chan)) {
      print(command)
      flush()
    }

    val reader = new InputStreamReader(Channels.newInputStream(chan))
    val result = CharBuffer.allocate(1024)
    reader.read(result)
    result.flip()
    result.toString
  }

  def stat(
    proxy: Proxy = Proxy.Any,
    statable: Statable = Statable.Any,
    serverId: Server = Server.Any) =
    request(s"show stat ${proxy.id} ${statable.typ} ${serverId.id};")

  def info() = request("show info;")

  def sess() = request("show sess;")

  def errors(fallible: Fallible = Fallible.Any) =
    request("show errors ${fallible.id};")
}
