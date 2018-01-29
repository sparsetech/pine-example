# Pine example
This is an example website that uses [Pine](https://github.com/sparsetech/pine)
for HTML rendering, [Trail](https://github.com/sparsetech/trail) for routing,
[Circe](https://github.com/circe/circe) for JSON handling and
[http4s](https://github.com/http4s/http4s) for serving pages.

The [chosen architecture](http://sparse.tech/docs/pine.html#architectures) is:

> Server-side rendered pages with client logic and client-side rendering

The initial rendering is performed by the server. Then, the browser only sets up
the event handlers. When the page changes, the client renders the page. This is
possible since we request the templates from the client and share the rendering
logic across the JavaScript and JVM backend. After the initial rendering, no
further server requests apart from API requests will be sent. As a consequence,
this architecture keeps the website responsive while reducing the server
bandwidth.

You can edit templates in `assets/html` and refresh the page. The server sets up
a file watcher that invalidates the template cache upon file changes. As long as
the IDs and element types remain the same, templates can be readily changed
without the need to restart the server.

## Usage
```scala
$ sbt
buildJVM/reStart
```

## Licence
Pine is licensed under the terms of the Apache v2.0 licence.

## Authors
* Tim Nieradzik
