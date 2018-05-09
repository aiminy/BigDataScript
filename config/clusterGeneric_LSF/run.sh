#!/bin/sh

# usage : 
# sh Aimin_project/BigDataScript/config/clusterGeneric_LSF/run.sh bbc CufflinksAnno 48:00 8 general 3686 echo "ok"
# /nethome/axy148/Aimin_project/BigDataScript/config/clusterGeneric_LSF/run.sh bbc CufflinksAnno 48:00 4 bigmem 3686 .bds/bds Aimin_project/AtacSeq/inst/bin/bds/test_4.bds

arg1=$1
arg2=$2
arg3=$3
arg4=$4
arg5=$5
arg6=$6

shift 6

echo $@ | bsub  \
       -P "$arg1" \
       -J "$arg2" \
	-e %J.$arg2.err \
	-o %J.$arg2.log \
	-W "$arg3" \
	-n "$arg4" \
	-q "$arg5" \
	-R "rusage[mem="$arg6"] span[ptile=16]"