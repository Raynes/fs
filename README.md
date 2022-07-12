# fs - File system utilities for Clojure

```clj
[fs "1.4.7"]
```

This library defines some utilities for working with the file system in Clojure. Mostly, it wants to fill the gap that
`clojure.java.io` leaves and add on (and prettify) what `java.io.File` provides.

## pre-commit

- Install: https://pre-commit.com/
- running locally: This will also happen automatically before committing to a branch, but you can also run the tasks with `pre-commit run --all-files`

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

## License

Copyright (C) 2010-2013 Miki Tebeka, Anthony Grimes

Distributed under the Eclipse Public License, the same as Clojure.
