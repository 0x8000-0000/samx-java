#!/bin/sh

sed -n '/^ *```/s/ *```(\(\w*\)).*(\#\([-a-z.0-9]*\))/\2.\1/p;' $1