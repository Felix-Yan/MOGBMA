#!/bin/sh
#
# Force Bourne Shell if not Sun Grid Engine default shell (you never know!)
#
#$ -S /bin/sh
#
# I know I have a directory here so I'll use it as my initial working directory
#
#$ -wd /vol/grid-solar/sgeusers/sawczualex 
#
# End of the setup directives
#
# Stdout from programs and shell echos will go into the file
#    scriptname.o$JOB_ID
#  so we'll put a few things in there to help us see what went on
#

# For testing locally
#JOB_ID=3
#SGE_TASK_ID=1

DIR_TMP="/local/tmp/sawczualex/$JOB_ID/"
DIR_HOME="/u/students/sawczualex/"
DIR_GRID=$DIR_HOME"grid/"
DIR_WORKSPACE="workspace/"
DIR_PROGRAM=$DIR_HOME$DIR_WORKSPACE/"GraphEvol/"
ECJ_JAR=$DIR_HOME$DIR_WORKSPACE/"Library/ecj.23.jar"
DIR_OUTPUT=$DIR_GRID$2 # Match this argument with dataset name

FILE_JOB_LIST="CURRENT_JOBS.txt"
FILE_RESULT_PREFIX="out"

   
mkdir -p $DIR_TMP

# Preliminary test to ensure that the directory has been created successfully.
if [ ! -d $DIR_TMP ]; then
  echo "Could not create the temporary directory for processing the job. "
  echo "/local/tmp/ directory: "
  ls -la /local/tmp
  echo "/local/tmp/sawczualex directory: "
  ls -la /local/tmp/sawczualex
  echo "Exiting"
  exit 1
fi

# Add the job ID to the list of jobs currently being processed.
echo $JOB_ID >> $DIR_GRID$FILE_JOB_LIST

# Copy the files required for processing into the temporary directory.
cp -r $DIR_PROGRAM"bin" $DIR_TMP
cp $DIR_PROGRAM"graph-evol.params" $DIR_TMP
cp $DIR_PROGRAM"graph-evol-newops.params" $DIR_TMP
cp $ECJ_JAR $DIR_TMP
cp $1/* $DIR_TMP # Copy datasets

mkdir -p $DIR_TMP"results"

cd $DIR_TMP

echo "Running: "

seed=$SGE_TASK_ID
result=$FILE_RESULT_PREFIX$seed.stat

java -cp ecj.23.jar:./bin:. ec.Evolve -file $3 -p seed.0=$seed -p stat.file=\$$result
cp $result ./results

# Now we move the output to a place to pick it up from later and clean up
cd results
if [ ! -d $DIR_OUTPUT ]; then
  mkdir $DIR_OUTPUT
fi
cp $DIR_TMP"results"/*.stat $DIR_OUTPUT

# Do the cleaning up from our starting directory
rm -rf /local/tmp/sawczualex/$JOB_ID

# Remove the job ID from the list of jobs currently being processed.
FILE_TMP=`date +%N`
grep -v "$JOB_ID" $DIR_GRID$FILE_JOB_LIST > $DIR_GRID$FILE_TMP
mv $DIR_GRID$FILE_TMP $DIR_GRID$FILE_JOB_LIST

# Finish the job.
echo "Ran through OK"


