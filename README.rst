======================================
fs - File system utilities for Clojure
======================================

About
=====
This library tries to provide Clojure code for handling the file system.

It has most of the functionality found in `java.io.File`_ influenced by Python's
`os.path`_ and `shutil`_.

.. _`java.io.File`: http://java.sun.com/javase/6/docs/api/java/io/File.html
.. _`os.path`: http://docs.python.org/library/os.path.html
.. _`shutil`: http://docs.python.org/library/shutil.html

API
===

abspath
    Return absolute path
basename
    Return the last part of path
chmod
    Set/unset permission on path
copy
    Copy a file
cwd
    Return the current working directory
delete
    Delete path
directory?
    True if path is a directory
dirname
    Return directory name
executable?
    Check if path is executable
exists?
    Check if path exists
file?
    True if path is a file
glob
    `ls` like operator
join
    Join part to path
listdir
    List files under directory
mkdir
    Create directory
mtime
    File modification time
mkdirs
    Create directory tree
normpath
    Return normalized (canonical) path
readable?
    Check if path is readable
rename
    Rename path
separator
    Path separator
size
    File size
split
    Split path to parts
tempdir
    Create temporary directory
tempfile 
    Create temporary file
touch
    Change file modification time
walk
    Walk over directory structure, calling function on every step
writeable?
    Check if path is writable

Authors
=======
Miki Tebeka <miki.tebeka@gmail.com>
Justin Kramer <jkkramer@gmail.com> (glob->regexp)

License
=======
Copyright (C) 2010 Miki Tebeka <miki.tebeka@gmail.com>

Distributed under the Eclipse Public License, the same as Clojure.
