SAMx
====

Semantic Authoring Markdown

Implementation of a SAM toolset in Java using an ANTLRv4 Grammar.

Please see [SAM Documentation](https://mbakeranalecta.github.io/sam/) for 
an overview and scope of SAM. Please refer to
https://github.com/mbakeranalecta/sam for the original implementation.

Dependencies
------------

A Java 1.8 compiler (tested with OpenJDK 11) and Antlr 4.8 (included as Gradle
dependency)


Building
--------

```shell script
$ ./gradlew assembleDist
```


What's the difference between SAM and SAMx?
-------------------------------------------

SAM is Mark Baker's original implementation, written in Python using a
hand-written parser.

SAMx-Java is a separate implementation in Java using a parser generator thus
intending to formalize the grammar. Also, this implementation will experiment
with more complex conditions attached to various flows and other elements.


Can SAMx parse SAM documents?
-----------------------------

There are certain differences between SAM and SAMx, some due to the
implementation details (hand-written parsers can be more expressive than
those generated by a parser generator, although ANTLR is very flexible) and
some due to explicit design choices:

* SAM record sets define the field names as part of the record set
description, while SAMx uses the record set description flow for the
equivalent of a table caption, and uses the first row as a header which
defines the field names.

* SAM record set field separator is comma, while SAMx uses a vertical pipe
symbol '|' .

* SAM allows for multiple conditions on a flow, and they are all logically
'and'-ed together. SAMx allows for one condition on a flow, but allows that
condition to be computed as a boolean expression of other conditions.

* SAM's annotations are in plain parentheses, attached without space to the
phrase they apply to. However since the SAMx parser ignores space, and
allowing an arbitrary mix of attributes and annotations attached to a phrase,
SAMx uses the '(:' syntax to introduce an annotation. This allows at this time
for parentheses to be freely used in text without having to escape them.

* SAM uses "p" for a paragraph in the XML serialization while SAMx uses "para"
for compatibility with DocBook.

* SAMx has support for conditional blocks, where a top-level condition 
applies to several sub-blocks at once.

* SAMx is using the CSS sigil for selecting name (hash mark) instead of SAM's
which is an asterisk. SAMx also supports a class attribute introduced by a dot.


License for sam
---------------

[Original SAM code is available](https://github.com/mbakeranalecta/sam/blob/master/license.txt)
under Apache 2.0 license or Eclipse Public License v1.0.

License for samx
----------------

Copyright 2020 Florin Iucha

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
