#!/bin/bash

mode=${1:-"--write"}
required_prettier_version=2.1.1
required_plugin_version=0.8.3
current_prettier_version=$(prettier --version) #just prints version

#NPM can't just print version, so:
#First operation is to print part of dependency tree containing prettier-plugin-java dependency.
#output should look like this (directory may be different):
##############
#/usr/lib
#└── prettier-plugin-java@0.7.1
##############
#next we just take second line from output
current_plugin_version=$(npm list -g --depth=0 prettier-plugin-java |
    sed -n '2 s/....prettier-plugin-java@// p' | # remove from result '└── prettier-plugin-java@' remaining version
    sed -n '1 s/[[:space:]]*$// p') # remove spaces from the end

if [ "$required_prettier_version" != "$current_prettier_version" ]
  then
    echo ERROR: You have incorrect prettier version.
    echo ERROR: Your version is "$current_prettier_version", but we use version "$required_prettier_version".
    echo ERROR: Install correct version by: "'npm install -g prettier@$required_prettier_version'".
    exit 1
fi

if [ "$required_plugin_version" != "$current_plugin_version" ]
  then
    echo ERROR: You have incorrect prettier-plugin-java version.
    echo Your version is "$current_plugin_version", but we use version "$required_plugin_version".
    echo ERROR: Install correct version by: "'npm install -g prettier-plugin-java@$required_plugin_version'".
    exit 1
fi

prettier $mode --tab-width 4 --print-width 120 --trailing-comma all --end-of-line auto src/**/*.java
