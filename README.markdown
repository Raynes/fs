# fs - File system utilities for Clojure

[![Build Status](https://secure.travis-ci.org/Raynes/fs.png)](http://travis-ci.org/Raynes/fs)

[API docs](http://raynes.github.com/fs/)

This library defines some utilities for working with the file system in Clojure. Mostly, it wants to fill the gap that
`clojure.java.io` leaves and add on (and prettify) what `java.io.File` provides.

## Usage

This library is simple. It is just a collection of functions that do things with the file system. The one thing
you should understand is `*cwd*`. This library wraps a lot of built-in Java file systemy things because it
pays attention to the `*cwd*` as the current working directory. Java has no way to change the cwd of a JVM so
if you want that behavior, you have to simulate it. This library tries to do that.

The foundation of the library is the `file` function. It is just like `clojure.java.io/file`, but it pays
attention to the value of `*cwd*`.

This is 100% a utility library. If you have something useful that it doesn't already have, open a pull request,
because I probably want it. Make sure you include tests. Also, make sure they pass.

fs is *not* an I/O utility library. We should try to keep things limited to file system activities.

## Artifacts

Library artifacts are [released to Clojars](https://clojars.org/me.raynes/fs). If you are using Maven, add the following repository
definition to your `pom.xml`:

``` xml
<repository>
  <id>clojars.org</id>
  <url>http://clojars.org/repo</url>
</repository>
```

### The Most Recent Release

With Leiningen:

    [me.raynes/fs "1.4.6"]


With Maven:

    <dependency>
      <groupId>me.raynes</groupId>
      <artifactId>fs</artifactId>
      <version>1.4.6</version>
    </dependency>

## License

Copyright (C) 2010-2013 Miki Tebeka, Anthony Grimes

Distributed under the Eclipse Public License, the same as Clojure.
