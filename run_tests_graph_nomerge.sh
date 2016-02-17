#!/bin/bash

datasetroot=~/workspace
datasets='wsc2008 wsc2009'
resultsroot=~/workspace/GraphEvolNoMergeControlResults
for d in $datasets
do
    for t in $(find ${datasetroot}/$d/ -mindepth 1 -type d)
    do
      set=$(basename $t)
      result_dir=${set}Results
      mkdir -p $resultsroot/$result_dir
      for i in $(seq 0 29)
      do
	java -cp Library/ecj.22.jar:GraphEvol/bin:. ec.Evolve -file ~/workspace/GraphEvol/graph-evol.params -p seed.0=${i} -p stat.file=${resultsroot}/${result_dir}/out${i}.stat -p composition-task=${t}/problem.xml -p composition-services=${t}/services-output.xml -p composition-taxonomy=${t}/taxonomy.xml -p overlap-enabled=false
      done
    done
done