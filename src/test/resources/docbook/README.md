Generating DocBook
==================

You can run the to_xml converter, using the -r option to set the root element to `book`.

Example session:

```shell script
$ tar xf ~/work/samxj/build/distributions/samxj-0.1.8.tar
$ ls
samxj-0.1.8
$ cd samxj-0.1.8/test/docbook
samxj-0.1.8/test/docbook$ ls
chapter1.samx  chapter2.samx  main.samx  README.md
samxj-0.1.8/test/docbook$ ../../bin/to_xml -r book -i main.samx -o draft.xml
samxj-0.1.8/test/docbook$ ls draft.xml
draft.xml
samxj-0.1.8/test/docbook$ dblatex draft.xml
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