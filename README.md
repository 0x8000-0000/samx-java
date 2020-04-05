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


What's the difference between SAM and SAMx?
-------------------------------------------

SAM is Mark Baker's original implementation, written in Python using a
hand-written parser.

SAMx is an separate implementation in C++ using a parser generator thus
intending to formalize the grammar. Also this implementation will experiment
with more complex conditions attached to various flows and other elements.

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
