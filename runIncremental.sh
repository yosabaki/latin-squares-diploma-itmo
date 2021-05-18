#!/bin/sh
export LD_LIBRARY_PATH=./cryptominisat/build/lib

ENCODER="java -jar ./latin-square-generator/build/libs/latin-square-generator-1.0-SNAPSHOT.jar";
INCREMENTAL="incremental/incremental"
encode="encode -e reduced_latin_onehot -i";
decode="decode -e reduced_latin_onehot -o";

inputs="inputs";
outputs="outputs";

$ENCODER $encode "$inputs/incrementalInput" -n 10 -k 3 -r 1;
$INCREMENTAL "$inputs/incrementalInput" "$outputs/incremental/incrementalOutput" $1;
