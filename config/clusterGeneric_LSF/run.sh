#!/bin/sh
echo "$7" | bsub  \
       -P $1 \
       -J $2 \
	-e %J.$2.log \
	-o %J.$2.err \
	-W $3 \
	-n $4 \
	-q $5 \
	-R "rusage[mem="$6"] span[ptile=8]"
