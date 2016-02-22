package ec.graph;

import java.util.ArrayList;

import ec.EvolutionState;
import ec.Individual;
import ec.multiobjective.nsga2.NSGA2Breeder;
import ec.multiobjective.nsga2.NSGA2Evaluator;
import ec.multiobjective.nsga2.NSGA2MultiObjectiveFitness;
import ec.util.SortComparator;
import ec.graph.GraphState;

public class MOGBMAEvaluator extends NSGA2Evaluator{
	/**
	 * This method overrides the namesake in NSGA2Evaluator. The difference is that 
	 * this will store the ranks in GraphState.
	 */
	 public Individual[] buildArchive(EvolutionState state, int subpop)
     {
     Individual[] dummy = new Individual[0];
     ArrayList ranks = assignFrontRanks(state.population.subpops[subpop]);
     ((GraphState)state).setRanks(ranks); ;
             
     ArrayList newSubpopulation = new ArrayList();
     int size = ranks.size();
     for(int i = 0; i < size; i++)
         {
         Individual[] rank = (Individual[])((ArrayList)(ranks.get(i))).toArray(dummy);
         assignSparsity(rank);
         if (rank.length + newSubpopulation.size() >= originalPopSize[subpop])
             {
             // first sort the rank by sparsity
             ec.util.QuickSort.qsort(rank, new SortComparator()
                 {
                 public boolean lt(Object a, Object b)
                     {
                     Individual i1 = (Individual) a;
                     Individual i2 = (Individual) b;
                     return (((NSGA2MultiObjectiveFitness) i1.fitness).sparsity > ((NSGA2MultiObjectiveFitness) i2.fitness).sparsity);
                     }

                 public boolean gt(Object a, Object b)
                     {
                     Individual i1 = (Individual) a;
                     Individual i2 = (Individual) b;
                     return (((NSGA2MultiObjectiveFitness) i1.fitness).sparsity < ((NSGA2MultiObjectiveFitness) i2.fitness).sparsity);
                     }
                 });

             // then put the m sparsest individuals in the new population
             int m = originalPopSize[subpop] - newSubpopulation.size();
             for(int j = 0 ; j < m; j++)
                 newSubpopulation.add(rank[j]);
                             
             // and bail
             break;
             }
         else
             {
             // dump in everyone
             for(int j = 0 ; j < rank.length; j++)
                 newSubpopulation.add(rank[j]);
             }
         }

     Individual[] archive = (Individual[])(newSubpopulation.toArray(dummy));
             
     // maybe force reevaluation
     NSGA2Breeder breeder = (NSGA2Breeder)(state.breeder);
     if (breeder.reevaluateElites[subpop])
         for(int i = 0 ; i < archive.length; i++)
             archive[i].evaluated = false;

     return archive;
     }
}
