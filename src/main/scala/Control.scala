package airtraffic

import java.io.{ File, InputStreamReader, PrintWriter }
import java.nio.CharBuffer
import java.nio.channels.Channels
import jnr.unixsocket.{ UnixSocketAddress, UnixSocketChannel }

object Control {

  /** http://cbonte.github.io/haproxy-dconv/configuration-1.4.html#9.1 */
  case class Stats(map: Map[String, String]) {
    def get(key: String) = map.get(key)
    def apply(key: String) = get(key).get
    lazy val pxname = apply("pxname")
    lazy val svname = apply("svname")
    lazy val qcur = apply("qcur")
    lazy val qmax = apply("qmax")
    lazy val scur = apply("scur")
    lazy val smax = apply("smax")
    lazy val slim = apply("slim")
    lazy val stot = apply("stot")
    lazy val bin = apply("bin")
    lazy val bout = apply("bout")
    lazy val dreq = apply("dreq")
    lazy val dresp = apply("dresp")
    lazy val ereq = apply("ereq")
    lazy val econ = apply("econ")
    lazy val eresp = apply("eresp")
    lazy val wretr = apply("wretr")
    lazy val wredis = apply("wredis")
    lazy val status = apply("status")
    lazy val weight = apply("weight")
    lazy val act = apply("act")
    lazy val bck = apply("bck")
    lazy val chkfail = apply("chkfail")
    lazy val chkdown = apply("chkdown")
    lazy val lastchg = apply("lastchg")
    lazy val downtime = apply("downtime")
    lazy val qlimit = apply("qlimit")
    lazy val pid = apply("pid")
    lazy val iid = apply("iid")
    lazy val sid = apply("sid")
    lazy val throttle = apply("throttle")
    lazy val lbtot = apply("lbtot")
    lazy val tracked = apply("tracked")
    // (0=frontend, 1=backend, 2=server, 3=socket)
    lazy val tpe = apply("type")
    lazy val rate = apply("rate")
    lazy val rateLim = apply("rate_lim")
    lazy val rateMax = apply("rate_max")
    lazy val checkStatus = apply("check_status")
    lazy val checkCode = apply("check_code")
    lazy val checkDuration = apply("check_duration")
    lazy val hrsp_1xx = apply("hrsp_1xx")
    lazy val hrsp_2xx = apply("hrsp_2xx")
    lazy val hrsp_3xx = apply("hrsp_3xx")
    lazy val hrsp_4xx = apply("hrsp_4xx")
    lazy val hrsp_5xx = apply("hrsp_5xx")
    lazy val hrsp_other = apply("hrsp_other")
    lazy val hanafail = apply("hanafail")
    lazy val req_rate = apply("req_rate")
    lazy val req_rate_max = apply("req_rate_max")
    lazy val req_tot = apply("req_tot")
    lazy val cli_abrt = apply("cli_abrt")
    lazy val srv_abrt = apply("srv_abrt")
  }

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

  def weight(backend: String, server: String, weight: Int /*0 to 256*/) = {
    def clamped = weight match {
      case under if under < 0 => 0
      case over if over > 256 => 256
      case ok => ok
    }
    request(s"set weight $backend/$server $clamped;")
  }

  def weight(backend: String, server: String, weight: Double) = {
    def clamped = weight match {
      case under if under < 0 => 0
      case over if over > 100 => 100
      case ok => ok
    }
    request(s"set weight $backend/$server $clamped%;")
  }

  /** http://cbonte.github.io/haproxy-dconv/configuration-1.5.html#9.2-show%20stat */
  def stat(
    proxy: Proxy       = Proxy.Any,
    statable: Statable = Statable.Any,
    serverId: Server   = Server.Any): Iterable[Stats] =
    request(s"show stat ${proxy.id} ${statable.typ} ${serverId.id};").split("\n").toList match {
      case _ :: Nil =>
        Nil
      case names :: stats =>
        val keys = names.replace("# ", "").split(",")
        stats.map { line =>
          Stats(keys.zip(line.split(",")).toMap)
        }
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
