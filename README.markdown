# fs - File system utilities for Clojure

This library defines some utilities for working with the file system in Clojure. Mostly, it wants to fill the gap that `clojure.java.io` leaves and add on (and prettify) what `java.io.File` provides.

## Usage

The library is simple. It is just a collection of functions that do things. The one thing you should understand is `cwd`. There is no way to change the working directory in Java. Because of this, fs simulates working directory changes within the library itself. Basically, there is a `cwd` atom that can be changed and then all `fs` functions will consider this the working directory. There is a `file` function that can be used to create `java.io.File` objects in the same way as `clojure.java.io`, except that if the path isn't absolute or the first argument is `"."`, it uses `cwd`.

Use Leiningen to get fs: `:dependencies [[fs "1.0.0"]]`.

This is 100% a utility library. If you have something useful that it doesn't already have, open a pull request, because I probably want it. Make sure you include tests. 

fs is *not* an I/O utility library. We should try to keep things limited to file system activities.

We also have Marginalia [docs](http://raynes.github.com/fs/).

## Contributors

This library was originally devised and maintained by Miki Tebeka, but [I](https://github.com/Raynes) took over for him.

* [Miki Tebeka](mailto:miki.tebeka@gmail.com)
* [Justin Kramer](mailto:jkkramer@gmail.com)
* Steve Miner
* [Bronsa](mailto:brobronsa@gmail.com)
* [Anthony Grimes](https://github.com/Raynes)

## License

Copyright (C) 2010,2011 Miki Tebeka, Anthony Grimes

Distributed under the Eclipse Public License, the same as Clojure.
