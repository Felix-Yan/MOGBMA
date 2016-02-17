package ec.graph;

import ec.EvolutionState;
import ec.Individual;
import ec.Population;
import ec.Subpopulation;
import ec.simple.SimpleBreeder;
import ec.util.Parameter;

public class MOGBMABreeder extends SimpleBreeder{
	
	/**The only difference between this and the NSGA2Breeder is that this 
	 * checks whether the population is null before checking reduceBy
	 */
	public void setup(final EvolutionState state, final Parameter base)
	{
		super.setup(state, base);
		// make sure SimpleBreeder's elites facility isn't being used
		for (int i = 0; i < elite.length; i++)  // we use elite.length here instead of pop.subpops.length because the population hasn't been made yet.
			if (usingElitism(i))
				state.output.warning("You're using elitism with NSGA2Breeder, which is not permitted and will be ignored.  However the reevaluate-elites parameter *will* bre recognized by NSGAEvaluator.",
						base.push(P_ELITE).push(""+i));
		if(state.population != null){
			for (int i = 0; i < state.population.subpops.length; i++)
				if (reduceBy[i] != 0)
					state.output.fatal("NSGA2Breeder does not support population reduction.", base.push(P_REDUCE_BY).push(""+i), null);
		}               
		if (sequentialBreeding) // uh oh, haven't tested with this
			state.output.fatal("NSGA2Breeder does not support sequential evaluation.",
					base.push(P_SEQUENTIAL_BREEDING));

		if (!clonePipelineAndPopulation)
			state.output.fatal("clonePipelineAndPopulation must be true for NSGA2Breeder.");
	}
	
	 /**
     * Override breedPopulation(). We take the result from the super method in
     * SimpleBreeder and append it to the old population. Hence, after
     * generation 0, every subsequent call to
     * <code>NSGA2Evaluator.evaluatePopulation()</code> will be passed a
     * population of 2x<code>originalPopSize</code> individuals.
     */
    public Population breedPopulation(EvolutionState state)
        {
        Population oldPop = (Population) state.population;
        Population newPop = super.breedPopulation(state);
        Individual[] combinedInds;
        Subpopulation[] subpops = oldPop.subpops;
        Subpopulation oldSubpop;
        Subpopulation newSubpop;
        int subpopsLength = subpops.length;

        for (int i = 0; i < subpopsLength; i++)
            {
            oldSubpop = oldPop.subpops[i];
            newSubpop = newPop.subpops[i];
            combinedInds = new Individual[oldSubpop.individuals.length + newSubpop.individuals.length];
            System.arraycopy(newSubpop.individuals, 0, combinedInds, 0,  newSubpop.individuals.length);
            System.arraycopy(oldSubpop.individuals, 0, combinedInds,  newSubpop.individuals.length, oldSubpop.individuals.length);
            newSubpop.individuals = combinedInds;
            }
        return newPop;
        }
}
