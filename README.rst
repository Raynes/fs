======================================
fs - File system utilities for Clojure
======================================

About
=====
This library tries to provide Clojure code for handling the file system.
It has most of the functionality found in java.io.File and Python's os.path

API
===

separator
    Path separator
listdir
    List files under directory
executable?
    Check if path is executable
readable?
    Check if path is readable
writeable?
    Check if path is writable
delete
    Delete path
exists?
    Check if path exists
abspath
    Return absolute path
normpath
    Return normalized (canonical) path
basename
    Return the last part of path
dirname
    Return directory name
directory?
    True if path is a directory
file?
    True if path is a file
mtime
    File modification time
size
    File size
mkdir
    Create directory
mkdirs
    Create directory tree
join
    Join part to path
split
    Split path to parts
rename
    Rename path
tempfile 
    Create temporary file
tempdir
    Create temporary directory
cwd
    Return the current working directory
walk
    Walk over directory structure, calling function on every step

License
=======
Copyright (C) 2010 Miki Tebeka <miki.tebeka@gmail.com>

Distributed under the Eclipse Public License, the same as Clojure.
