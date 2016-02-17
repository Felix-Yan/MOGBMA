#!/bin/bash

need R
resultsroot=~/workspace/GraphEvolMergeControlResults
echo "MeanFitness sdFitness meanTime sdTime directory"
for d in $(find $resultsroot -mindepth 1 -type d)
do
  cd $d
  ~/workspace/folderStats_graphEvol.R
done





