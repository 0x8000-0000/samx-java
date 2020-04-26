Generating DocBook
==================

You can run the to_xml converter, using the -r option to set the root element to `book`.

Convert test files to DocBook XML:

```shell script
$ tar xf ~/work/samxj/build/distributions/samxj-0.4.4.tar
$ ls
samxj-0.4.4
$ cd samxj-0.4.4/examples/docbook
samxj-0.4.4/examples/docbook$ ls
chapter1.samx  chapter2.samx  main.samx  README.md
samxj-0.4.4/examples/docbook$ ../../bin/to_xml -i main.samx -o draft.xml -r book -n http://docbook.org/ns/docbook -v 5.1 --docbook -s ../../schemas/docbook.rng.gz
Enable DocBook mode
XML output is well-formed
DocBook document validated using Jing
samxj-0.4.4/examples/docbook$ ls draft.xml
draft.xml
```

The converting step also validates the output against the included DocBook schema.

Generate PDF from DocBook XML:

```shell script
samxj-0.4.4/test/docbook$ dblatex draft.xml
Build the book set list...
Build the listings...
XSLT stylesheets DocBook - LaTeX 2e (0.3.11py3)
===================================================
Stripping NS from DocBook 5/NG document.
Processing stripped document.
Build draft.pdf
'draft.pdf' successfully built
```
