package airtraffic

import java.io.{ File, InputStreamReader, PrintWriter }
import java.nio.CharBuffer
import java.nio.channels.Channels
import jnr.unixsocket.{ UnixSocketAddress, UnixSocketChannel }

object Control {
  
  // # pxname,svname,qcur,qmax,scur,smax,slim,stot,bin,bout,dreq,dresp,ereq,econ,eresp,wretr,wredis,status,weight,act,bck,chkfail,chkdown,lastchg,downtime,qlimit,pid,iid,sid,throttle,lbtot,tracked,type,rate,rate_lim,rate_max,check_status,check_code,check_duration,hrsp_1xx,hrsp_2xx,hrsp_3xx,hrsp_4xx,hrsp_5xx,hrsp_other,hanafail,req_rate,req_rate_max,req_tot,cli_abrt,srv_abrt,
  //case class Stat()

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

/** see section 9.2 (  Unix Socket commands ) of http://cbonte.github.io/haproxy-dconv/configuration-1.5.html#9.2
 *  @param path is a file to the unix domain socket defined on your haproxy configs `stats socket <path>` */
case class Control(path: File) {
  import airtraffic.Control._

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

  def map(name: String)(key: String, value: String) =
    request(s"map $name $key $value;")

  def mapSet(name: String, key: String)(value: String) =
    request(s"map set $name $key $value;")

  def clearMap(name: String) =
    request("clear map $name;")

  def clearCounters(all: Boolean = false) =
    request(s"clear counters${if (all) " all" else ""};")

  // todo [ data.<type> <operator> <value> ] | [ key <key> ]
  def clearTable(tbl: String) =
    request(s"clear table $tbl;")

  def deleteMap(name: String)(key: String) =
    request(s"del map $name $key;")

  def disableAgent(backend: String, server: String) =
    request(s"disable agent $backend/$server;")

  def disableFrontend(name: String) =
    request(s"disable frontend $name;")

  def shutdownFrontend(fe: String) =
    request(s"shutdown frontend $fe;")

  def enableFrontend(name: String) =
    request(s"enable frontend $name;")

  def maxFrontendConnections(fe: String, max: Int) =
    request(s"set maxcon frontend $fe $max;")

  def disableServer(backend: String, server: String) =
    request(s"disable server $backend/$server;")

  def enableAgent(backend: String, server: String) =
    request(s"enable agent $backend/$server;")

  def enableServer(backend: String, server: String) =
    request(s"enable server $backend/$server;")

  def weight(backend: String, server: String) =
    request(s"get weight $backend/$server;")

  def help =
    request(s"help;")

  def maxGlobalConnections(max: Int) =
    request(s"set maxconn global $max;")

  def rateLimitGlobalConnections(max: Int) =
    request(s"set rate-limit connections global $max;")

  def rateLimitGlobalHttpCompression(max: Int) =
    request(s"set rate-limit http-compression global $max;")

  def rateLimitGlobalSessions(max: Int, ssl: Boolean = false) =
    request(s"set rate-limit ${if (ssl) "ssl-" else ""}sessions global $max;")

  def weight(backend: String, server: String, weight: Int /*0 to 256*/) =
    request(s"set weight $backend/$server $weight;")

  /** http://cbonte.github.io/haproxy-dconv/configuration-1.5.html#9.2-show%20stat */
  def stat(
    proxy: Proxy       = Proxy.Any,
    statable: Statable = Statable.Any,
    serverId: Server   = Server.Any): Iterable[Map[String, String]] =
    request(s"show stat ${proxy.id} ${statable.typ} ${serverId.id};").split("\n").toList match {
      case _ :: Nil =>
        Nil
      case names :: stats =>
        val keys = names.replace("# ", "").split(",")
        stats.map { line => keys.zip(line.split(",")).toMap }
    }

  def info() = request("show info;")

  def sess(id: String = "") = request(s"show sess $id;")

  def errors(fallible: Fallible = Fallible.Any) =
    request(s"show errors ${fallible.id};")

  def shutdownSession(id: String) =
    request(s"shutdown session $id;")

  def shutdownSessions(backend: String, server: String) =
    request(s"shutdown sessions $backend/$server;")
}
