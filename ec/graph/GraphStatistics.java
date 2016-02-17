package ec.graph;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ec.EvolutionState;
import ec.Individual;
import ec.simple.SimpleShortStatistics;
import ec.util.Parameter;

/**
 *
 * @author Alex
 */
public class GraphStatistics extends SimpleShortStatistics {
    public int histogramLog = 0; // 0 by default means stdout

//    @Override
//    public void setup( final EvolutionState state, final Parameter base ) {
//        super.setup( state, base );
//        File histogramFile = GraphInitializer.histogramLogFile;
//        if ( histogramFile != null ) try {
//            histogramLog = state.output.addLog( histogramFile, true, false, false );
//        }
//        catch ( IOException i ) {
//            state.output.fatal( "An IOException occurred trying to create the log " + histogramFile + ":\n" + i );
//        }
//        // else we�ll just keep the log at 0, which is stdout
//    }

    public void createHistogramLog( final EvolutionState state ) {
        File histogramFile = GraphInitializer.histogramLogFile;
        if ( histogramFile != null ) try {
            histogramLog = state.output.addLog( histogramFile, true, false, false );
        }
        catch ( IOException i ) {
            state.output.fatal( "An IOException occurred trying to create the log " + histogramFile + ":\n" + i );
        }
        // else we�ll just keep the log at 0, which is stdout
    }

    @Override
    public void postEvaluationStatistics(EvolutionState state){
        boolean output = (state.generation % modulus == 0);

        // gather timings
        if (output && doTime)
            {
            Runtime r = Runtime.getRuntime();
            long curU =  r.totalMemory() - r.freeMemory();
            state.output.print("" + (System.currentTimeMillis()-lastTime) + " ",  statisticslog);
            }

        int subpops = state.population.subpops.length;                          // number of supopulations
        totalIndsThisGen = new long[subpops];                                           // total assessed individuals
        bestOfGeneration = new Individual[subpops];                                     // per-subpop best individual this generation
        totalSizeThisGen = new long[subpops];                           // per-subpop total size of individuals this generation
        totalFitnessThisGen = new double[subpops];                      // per-subpop mean fitness this generation
        double[] meanFitnessThisGen = new double[subpops];                      // per-subpop mean fitness this generation


        prepareStatistics(state);

        // gather per-subpopulation statistics

        for(int x=0;x<subpops;x++)
            {
            for(int y=0; y<state.population.subpops[x].individuals.length; y++)
                {
                if (state.population.subpops[x].individuals[y].evaluated)               // he's got a valid fitness
                    {
                    // update sizes
                    long size = state.population.subpops[x].individuals[y].size();
                    totalSizeThisGen[x] += size;
                    totalSizeSoFar[x] += size;
                    totalIndsThisGen[x] += 1;
                    totalIndsSoFar[x] += 1;

                    // update fitness
                    if (bestOfGeneration[x]==null ||
                        state.population.subpops[x].individuals[y].fitness.betterThan(bestOfGeneration[x].fitness))
                        {
                        bestOfGeneration[x] = state.population.subpops[x].individuals[y];
                        if (bestSoFar[x]==null || bestOfGeneration[x].fitness.betterThan(bestSoFar[x].fitness))
                            bestSoFar[x] = (Individual)(bestOfGeneration[x].clone());
                        }

                    // sum up mean fitness for population
                    totalFitnessThisGen[x] += state.population.subpops[x].individuals[y].fitness.fitness();

                    // hook for KozaShortStatistics etc.
                    gatherExtraSubpopStatistics(state, x, y);
                    }
                }
            // compute mean fitness stats
            meanFitnessThisGen[x] = (totalIndsThisGen[x] > 0 ? totalFitnessThisGen[x] / totalIndsThisGen[x] : 0);

            // hook for KozaShortStatistics etc.
            if (output && doSubpops) printExtraSubpopStatisticsBefore(state, x);

            // print out optional average size information
            if (output && doSize && doSubpops)
                {
                state.output.print("" + (totalIndsThisGen[x] > 0 ? ((double)totalSizeThisGen[x])/totalIndsThisGen[x] : 0) + " ",  statisticslog);
                state.output.print("" + (totalIndsSoFar[x] > 0 ? ((double)totalSizeSoFar[x])/totalIndsSoFar[x] : 0) + " ",  statisticslog);
                state.output.print("" + (double)(bestOfGeneration[x].size()) + " ", statisticslog);
                state.output.print("" + (double)(bestSoFar[x].size()) + " ", statisticslog);
                }

            // print out fitness information
            if (output && doSubpops)
                {
                state.output.print("" + meanFitnessThisGen[x] + " ", statisticslog);
                state.output.print("" + bestOfGeneration[x].fitness.fitness() + " ", statisticslog);
                state.output.print("" + bestSoFar[x].fitness.fitness() + " ", statisticslog);
                }

            // hook for KozaShortStatistics etc.
            if (output && doSubpops) printExtraSubpopStatisticsAfter(state, x);
            }



        // Now gather per-Population statistics
        long popTotalInds = 0;
        long popTotalIndsSoFar = 0;
        long popTotalSize = 0;
        long popTotalSizeSoFar = 0;
        double popMeanFitness = 0;
        double popTotalFitness = 0;
        Individual popBestOfGeneration = null;
        Individual popBestSoFar = null;
        int path = 0;
        int numNodes = 0;

        for(int x=0;x<subpops;x++)
            {
            popTotalInds += totalIndsThisGen[x];
            popTotalIndsSoFar += totalIndsSoFar[x];
            popTotalSize += totalSizeThisGen[x];
            popTotalSizeSoFar += totalSizeSoFar[x];
            popTotalFitness += totalFitnessThisGen[x];
            if (bestOfGeneration[x] != null && (popBestOfGeneration == null || bestOfGeneration[x].fitness.betterThan(popBestOfGeneration.fitness)))
                popBestOfGeneration = bestOfGeneration[x];
            if (bestSoFar[x] != null && (popBestSoFar == null || bestSoFar[x].fitness.betterThan(popBestSoFar.fitness))) {
                popBestSoFar = bestSoFar[x];
                path = ((GraphIndividual) popBestSoFar).longestPathLength;
                numNodes = ((GraphIndividual) popBestSoFar).numAtomicServices;
            }

            // hook for KozaShortStatistics etc.
            gatherExtraPopStatistics(state, x);
            }

        // build mean
        popMeanFitness = (popTotalInds > 0 ? popTotalFitness / popTotalInds : 0);               // average out

        // hook for KozaShortStatistics etc.
        if (output) printExtraPopStatisticsBefore(state);

        // optionally print out mean size info
        if (output && doSize)
            {
            state.output.print("" + (popTotalInds > 0 ? popTotalSize / popTotalInds : 0)  + " " , statisticslog);                                           // mean size of pop this gen
            state.output.print("" + (popTotalIndsSoFar > 0 ? popTotalSizeSoFar / popTotalIndsSoFar : 0) + " " , statisticslog);                             // mean size of pop so far
            state.output.print("" + (double)(popBestOfGeneration.size()) + " " , statisticslog);                                    // size of best ind of pop this gen
            state.output.print("" + (double)(popBestSoFar.size()) + " " , statisticslog);                           // size of best ind of pop so far
            }

        // print out fitness info
        if (output)
            {
            state.output.print("" + popMeanFitness + " " , statisticslog);        // mean fitness of pop this gen
            state.output.print("" + (popBestOfGeneration.fitness.fitness()) + " " , statisticslog);                 // best fitness of pop this gen
            state.output.print("" + (popBestSoFar.fitness.fitness()) + " " , statisticslog);                // best fitness of pop so far
            state.output.print("" + numNodes + " ", statisticslog);
            state.output.print("" + path + " ", statisticslog);

            //Also print the A, R, C, T values of popBestOfGeneration & popBestSoFar
            state.output.print("BestOfGeneration(ARCT): ", statisticslog);
            state.output.print(((GraphIndividual) popBestOfGeneration).getAvailability()+" ", statisticslog);
            state.output.print(((GraphIndividual) popBestOfGeneration).getReliability()+" ", statisticslog);
            state.output.print(((GraphIndividual) popBestOfGeneration).getCost()+" ", statisticslog);
            state.output.print(((GraphIndividual) popBestOfGeneration).getTime()+" ", statisticslog);
            state.output.print("BestSoFar(ARCT): ", statisticslog);
            state.output.print(((GraphIndividual) popBestSoFar).getAvailability()+" ", statisticslog);
            state.output.print(((GraphIndividual) popBestSoFar).getReliability()+" ", statisticslog);
            state.output.print(((GraphIndividual) popBestSoFar).getCost()+" ", statisticslog);
            state.output.print(((GraphIndividual) popBestSoFar).getTime()+" ", statisticslog);
            }

        // hook for KozaShortStatistics etc.
        if (output) printExtraPopStatisticsAfter(state);

        // we're done!
        if (output) state.output.println("", statisticslog);

        // Now let's write the histogram log
        if (output) {
            // Print the best candidate at the end of the run
            if (state.generation == state.parameters.getInt(new Parameter("generations"), null)-1) {
                state.output.println(popBestSoFar.toString(), statisticslog);
                state.output.println("nodeOpt "+((GraphState)state).getTotalNodeOpt(), statisticslog);
                state.output.println("edgeOpt "+((GraphState)state).getTotalEdgeOpt(), statisticslog);
                createHistogramLog(state);
                createHistogramLog(state);
                //debug, this will print out the all a and r values in each service component of the final solution
                /*for(Node n: ((GraphIndividual) popBestSoFar).considerableNodeMap.values()){
                	double[] qos = n.getQos();
        			System.out.println("A: "+qos[GraphInitializer.AVAILABILITY]);
                }
                for(Node n: ((GraphIndividual) popBestSoFar).considerableNodeMap.values()){
                	double[] qos = n.getQos();
        			System.out.println("R: "+qos[GraphInitializer.RELIABILITY]);
                }*/

                // Write node histogram
                List<String> keyList = new ArrayList<String>(GraphInitializer.nodeCount.keySet());
                Collections.sort( keyList );

                for (String key : keyList)
                    state.output.print( key + " ", histogramLog );
                state.output.println( "", histogramLog );

                for (String key : keyList)
                    state.output.print( String.format("%d ", GraphInitializer.nodeCount.get( key )), histogramLog );
                state.output.println( "", histogramLog );

                // Write edge histogram
                List<String> edgeList = new ArrayList<String>(GraphInitializer.edgeCount.keySet());
                Collections.sort( edgeList );

                for (String key : edgeList)
                    state.output.print( key + " ", histogramLog );
                state.output.println( "", histogramLog );

                for (String key : edgeList)
                    state.output.print( String.format("%d ", GraphInitializer.edgeCount.get( key )) , histogramLog);
                state.output.println( "", histogramLog );
            }
        }
    }
}
