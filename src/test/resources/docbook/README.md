Generating DocBook
==================

You can run the to_xml converter, using the -r option to set the root element to `book`.

Convert test files to DocBook XML:

```shell script
$ tar xf ~/work/samxj/build/distributions/samxj-0.1.9.tar
$ ls
samxj-0.1.9
$ cd samxj-0.1.8/test/docbook
samxj-0.1.9/test/docbook$ ls
chapter1.samx  chapter2.samx  main.samx  README.md
samxj-0.1.9/test/docbook$ ../../bin/to_xml -i main.samx -o draft.xml -r book -n http://docbook.org/ns/docbook -v 5.1
samxj-0.1.9/test/docbook$ ls draft.xml
draft.xml
```

Validate DocBook v5.1 using Jing:

```shell script
samxj-0.1.9/test/docbook$ wget https://docbook.org/xml/5.1/rng/docbook.rng
samxj-0.1.9/test/docbook$ jing -f docbook.rng foo.xml
[warning] /usr/bin/jing: Unable to locate batik-all in /usr/share/java
```

Generate PDF from DocBook XML:

```shell script
samxj-0.1.9/test/docbook$ dblatex draft.xml
Build the book set list...
Build the listings...
XSLT stylesheets DocBook - LaTeX 2e (0.3.11py3)
===================================================
No template matches p in chapter.
No template matches p in section.
No template matches p in chapter.
No template matches p in section.
Build draft.pdf
'draft.pdf' successfully built
```