#!/bin/sh

# usage : 
# sh Aimin_project/BigDataScript/config/clusterGeneric_LSF/run.sh
# /nethome/axy148/Aimin_project/BigDataScript/config/clusterGeneric_LSF/run.sh bbc CufflinksAnno 48:00 4 bigmem 3686 .bds/bds Aimin_project/AtacSeq/inst/bin/bds/test_4.bds

arg1=$1
arg2=$2
arg3=$3
arg4=$4
arg5=$5
arg6=$6
arg7=$7

arg22=$((arg2/3600))
arg44=$((arg4/1024))

shift 7

echo $@ | bsub  \
        -P "$arg1" \
        -W "$arg22" \
        -n "$arg3" \
        -R "rusage[mem="$arg44"] span[ptile=16]" \
        -q "$arg5" \
        -o "$arg6" \
        -e "$arg7"
