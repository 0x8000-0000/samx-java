Getting Started with SAMx
=========================

Build the Distribution Package
------------------------------

```shell script
# Check Java version
$ java -version
openjdk version "11.0.7-ea" 2020-04-14
OpenJDK Runtime Environment (build 11.0.7-ea+9-post-Debian-1)
OpenJDK 64-Bit Server VM (build 11.0.7-ea+9-post-Debian-1, mixed mode, sharing)

# Clone the repository
$ git clone https://github.com/0x8000-0000/samx-java/
Cloning into 'samx-java'...
remote: Enumerating objects: 20, done.
remote: Counting objects: 100% (20/20), done.
remote: Compressing objects: 100% (12/12), done.
remote: Total 1778 (delta 0), reused 14 (delta 0), pack-reused 1758
Receiving objects: 100% (1778/1778), 290.76 KiB | 2.57 MiB/s, done.
Resolving deltas: 100% (661/661), done.
$ cd samx-java

# Build and package
$ ./gradlew clean build assemble


> Task :generateBook
Enable DocBook mode
XML output is well-formed
DocBook document validated using Jing

BUILD SUCCESSFUL in 5s
18 actionable tasks: 18 executed

# Check build results
$ ls -l build/distributions/samxj*      
total 42780
-rw-r--r-- 1 florin florin 22917120 Apr 26 16:46 samxj-0.4.4.tar
-rw-r--r-- 1 florin florin 20888188 Apr 26 16:46 samxj-0.4.4.zip
```

Extract the Distribution Package
--------------------------------

```shell script
$ cd ..
$ tar xf samx-java/build/distributions/samxj-0.4.4.tar
$ cd samxj-0.4.4
$ ./bin/pretty_print examples/nested_typed_blocks-pretty.samx 
sample: alpha beta

   some text in here another entry

   subsection: Two

      more paragraphs follow

      subsubsubsection: Deep

         Super califragilistic expialidocious

         You sound quite precocious

   single line; at sample level.

OK: Prettified output matched input
```

Use the built-in apps
---------------------

There are several applications bundled in the distribution package:

```shell script
$ ls bin | cat
generate_header
generate_header.bat
pretty_print
pretty_print.bat
samxj
samxj.bat
to_html
to_html.bat
tokenize
tokenize.bat
to_xml
to_xml.bat
```

The applications can run on either UNIX or Windows.

Pretty-print
------------

`pretty_print` parses the input document and writes out a copy in canonical form: it normalizes white space by
replacing consecutive white spaces with one space, removes redundant empty lines and pads the table cell.

This tool can fully parse any valid SAMx input and reproduce it.

`samxj` is an alias for `pretty_print`.

To-XML
------

`to_xml` converts the input document into the corresponding XML format. It should support most of the SAMx constructs.

Debug Utilities
---------------

`tokenize` dumps the token stream of the input document.

In-development Utilities
------------------------

`to_html` converts the input document into the corresponding HTML format. It is under heavy development at this time
and it does not support all SAMx functionality.

`generate_header` is part of the support for literate programing in SAMx, where we define enumerations and data
structures inside SAMx, then generate both end-user documentation and source code from the same model.
