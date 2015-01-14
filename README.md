# airtraffic

> control your airspace

<p align="center">
  <img src="https://gimmebar-assets.s3.amazonaws.com/5355f055a0330.jpg"/>
</p>

# usage

Airtraffic scala interface for interacting with live instances of [HAProxy](http://haproxy.1wt.eu/) relying on haproxy's unix domain socket api.

```scala
import airtraffic.Control

val ctl = Control(statsFilePath)

// lists frontend, backend, listener info
ctl.stat().map(_.pxname).foreach(println)

// change the "weight" of a backend server
ctl.weight("backend-name", "server-name", Control.Weight.abs(weight))

// take a front end out of rotation 
ctl.disableFrontend("front-end-name")

// put a front end back into rotation
ctl.enableFrontend("front-end-name")
```

Doug Tangren (softprops) 2014-2015
