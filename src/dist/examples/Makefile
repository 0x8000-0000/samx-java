# Example documentation flow
#
# (Tested on Ubuntu 18.04)
#
# Prerequisites:
# 	sudo apt install xsltproc fop docbook-xsl-ns

TOOLS_PATH=$(abspath $(dir $(realpath $(firstword $(MAKEFILE_LIST))))..)
STYLESHEETS=/usr/share/xml/docbook/stylesheet/docbook-xsl-ns

SOURCE_FILE=$(MAKECMDGOALS:.pdf=.samx)
DEPENDENCIES=$(shell $(TOOLS_PATH)/bin/list_figures.sh $(SOURCE_FILE))

%.dbk: %.samx
	$(TOOLS_PATH)/bin/to_xml -b -i $^ -o $@ -s $(TOOLS_PATH)/schemas/docbook.rng.gz

$(DEPENDENCIES): $(SOURCE_FILE)
	$(TOOLS_PATH)/bin/extract_code -i $(SOURCE_FILE) -o .

.INTERMEDIATE: $(DEPENDENCIES)

%.png: %.plantuml
	java -jar $(TOOLS_PATH)/lib/plantuml-1.2020.8.jar $^

%.fo: %.dbk 
	xsltproc -o $@ $(STYLESHEETS)/fo/docbook.xsl $<

%.pdf: %.fo $(DEPENDENCIES:.plantuml=.png)
	fop -pdf $@ -fo $<

clean:
	$(RM) *.fo *.img *.pdf *.plantuml

.PHONY: clean
