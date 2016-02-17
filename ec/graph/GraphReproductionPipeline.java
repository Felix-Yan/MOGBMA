package ec.graph;

import ec.EvolutionState;
import ec.Individual;
import ec.breed.ReproductionPipeline;

/**
 * 
 * @author Alex
 */
public class GraphReproductionPipeline extends ReproductionPipeline {

    @Override
    public int produce(int min, int max, int start, int subpopulation, Individual[] inds, EvolutionState state, int thread) {
        GraphInitializer init = (GraphInitializer) state.initializer;
        
        int n = sources[0].produce(min, max, start, subpopulation, inds, state, thread);
        
        // Perform reproduction
        for(int q=start;q<n+start;q++) {
            GraphIndividual graph = (GraphIndividual)inds[q];
            init.countGraphElements( graph );
        }
        
        return super.produce(min, max, start, subpopulation, inds, state, thread);
    }
}
