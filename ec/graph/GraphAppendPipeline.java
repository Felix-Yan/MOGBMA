package ec.graph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ec.BreedingPipeline;
import ec.EvolutionState;
import ec.Individual;
import ec.util.Parameter;

public class GraphAppendPipeline extends BreedingPipeline {

	@Override
	public Parameter defaultBase() {
		return new Parameter("graphappendpipeline");
	}

	@Override
	public int numSources() {
		return 1;
	}

	@Override
	public int produce(int min, int max, int start, int subpopulation,
			Individual[] inds, EvolutionState state, int thread) {

		GraphInitializer init = (GraphInitializer) state.initializer;

		int n = sources[0].produce(min, max, start, subpopulation, inds, state, thread);

        if (!(sources[0] instanceof BreedingPipeline)) {
            for(int q=start;q<n+start;q++)
                inds[q] = (Individual)(inds[q].clone());
        }

        if (!(inds[start] instanceof GraphIndividual))
            // uh oh, wrong kind of individual
            state.output.fatal("GraphAppendPipeline didn't get a GraphIndividual. The offending individual is in subpopulation "
            + subpopulation + " and it's:" + inds[start]);

        // Perform mutation
        for(int q=start;q<n+start;q++) {
            GraphIndividual graph = (GraphIndividual)inds[q];


            // Determine whether you want to append a node by the inputs or by the output(s)
            if (init.random.nextBoolean()) {
            	// From the inputs
            	Map<String, Edge> connections = new HashMap<String, Edge>();

				candidateLoop: for (Node candidate : graph.unused) {
					candidate = candidate.clone();
					candidate.setConsidered(false);
					connections.clear();
					for (String i : candidate.getInputs()) {
						boolean found = false;

						for (Node service : init.taxonomyMap.get(i).servicesWithOutput) {
							if (graph.nodeMap.containsKey(service.getName())) {
								Set<String> intersect = new HashSet<String>();
								intersect.add(i);
								Edge mapEdge = connections.get(graph.nodeMap
										.get(service.getName()));
								if (mapEdge == null) {
									Edge e = new Edge(intersect);
									e.setConsidered(false);
									e.setFromNode(graph.nodeMap.get(service
											.getName()));
									e.setToNode(candidate);
									connections.put(e.getFromNode().getName(),
											e);
								} else
									mapEdge.getIntersect().addAll(intersect);

								found = true;
								break;
							}

						}
						// If that input cannot be satisfied, move on to another
						// candidate node to connect
						if (!found) {
							// Move on to another candidate
							continue candidateLoop;
						}
					}
					// Connect candidate to graph
					((GraphSpecies)graph.species).appendCandidateToGraphByInputs(candidate, connections, graph);
					break;
				}
			}
            else {
            	// By the output(s)
            	Map<String, Edge> connections = new HashMap<String, Edge>();

            	Node candidate = null;
            	for (Node c : graph.unused) {
            		candidate = c.clone();
            		candidate.setConsidered(false);
            		for (String o: candidate.getOutputs()) {
						for (Node service : init.taxonomyMap.get(o).servicesWithInput.keySet()) {

							if (graph.nodeMap.containsKey(service.getName()) && !connections.containsKey(service.getName())) {
								Set<String> intersect = new HashSet<String>();
								intersect.add(o);

								Edge e = new Edge(intersect);
								e.setConsidered(false);
								e.setFromNode(candidate);
								e.setToNode(graph.nodeMap.get(service.getName()));
								connections.put(e.getToNode().getName(), e);
								// Move on to the next output
								break;
							}
						}
            		}
            		// If at least one node output got connected, stop
            		if (!connections.isEmpty()) {
            			break;
            		}
            	}

				// Connect candidate to graph
            	if (candidate != null)
            		((GraphSpecies)graph.species).appendCandidateToGraphByOutputs(candidate, connections, graph);
            }



            graph.evaluated=false;
        }
        return n;
	}

}
