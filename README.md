# airtraffic

> control your airspace

# usage

Airtraffic scala interface for interacting with live instances of [HAProxy](http://haproxy.1wt.eu/) relying on haproxy's unix domain socket api.

```scala
val ctl = airtraffic.Control(statsFilePath)

// lists frontend, backend, listener info
ctl.info()

// change the "weight" of a backend server
ctl.weight("backend-name", "server-name", weight)

// take a front end out of rotation 
ctl.disableFrontend("front-end-name")

// put a front end back into rotation
ctl.enableFrontend("front-end-name")
```

Doug Tangren (softprops) 2013
