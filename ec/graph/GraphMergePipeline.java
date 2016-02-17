package ec.graph;

import java.util.HashSet;
import java.util.Set;

import ec.BreedingPipeline;
import ec.EvolutionState;
import ec.Individual;
import ec.util.Parameter;

public class GraphMergePipeline extends BreedingPipeline {
	int count;//debug

	@Override
	public Parameter defaultBase() {
		return new Parameter("graphmergepipeline");
	}

	@Override
	public int numSources() {
		return 2;
	}

	@Override
	public int produce(int min, int max, int start, int subpopulation,
			Individual[] inds, EvolutionState state, int thread) {

		GraphInitializer init = (GraphInitializer) state.initializer;

		Individual[] inds1 = new Individual[inds.length];
		Individual[] inds2 = new Individual[inds.length];

		int n1 = sources[0].produce(min, max, 0, subpopulation, inds1, state, thread);
		int n2 = sources[1].produce(min, max, 0, subpopulation, inds2, state, thread);

		if (!(sources[0] instanceof BreedingPipeline)) {
			for(int q=0;q<n1;q++)
				inds1[q] = (Individual)(inds1[q].clone());
		}

		if (!(sources[1] instanceof BreedingPipeline)) {
			for(int q=0;q<n2;q++)
				inds2[q] = (Individual)(inds2[q].clone());
		}

		if (!(inds1[0] instanceof GraphIndividual))
			// uh oh, wrong kind of individual
			state.output.fatal("GraphMergePipeline didn't get a GraphIndividual. The offending individual is in subpopulation "
					+ subpopulation + " and it's:" + inds1[0]);

		if (!(inds2[0] instanceof GraphIndividual))
			// uh oh, wrong kind of individual
			state.output.fatal("GraphMergePipeline didn't get a GraphIndividual. The offending individual is in subpopulation "
					+ subpopulation + " and it's:" + inds2[0]);

		int nMin = Math.min(n1, n2);

		// Perform crossover
		for(int q=start,x=0; q < nMin + start; q++,x++) {
			GraphIndividual g1 = ((GraphIndividual)inds1[x]);
			GraphIndividual g2 = ((GraphIndividual)inds2[x]);

			if (!init.overlapEnabled || enoughOverlap(g1, g2, init.overlapPercentage)) {
				GraphIndividual newG = mergeGraphs(g1, g2, init);
				GraphSpecies species = (GraphSpecies) newG.species;
				inds[q] = species.createNewGraph(newG, state, init.startNode.clone(), init.endNode.clone(), init.relevant);
			}
			else {
				if (g1.fitness.fitness() > g2.fitness.fitness())
					inds[q] = g1;
				else
					inds[q] = g2;
			}

			inds[q].evaluated=false;
			init.countGraphElements( (GraphIndividual) inds[q] );
			//debug
			/*if(!((GraphIndividual)inds[q]).validation()){
				throw new IllegalArgumentException("Graph's edges and nodes are not consistent");
			}*/
			//debug
			//System.out.println(count+"Merge");
			/*if(count == 90){
				System.out.println("time to debug");
			}*/
			count++;
		}
		return n1;
	}

	private GraphIndividual mergeGraphs(GraphIndividual g1, GraphIndividual g2, GraphInitializer init) {
		GraphIndividual newG = new GraphIndividual();
		// Merge nodes
		for (Node n: g1.nodeMap.values()) {
			newG.nodeMap.put(n.getName(), n.clone());
			newG.considerableNodeMap.put(n.getName(), n.clone());
		}
		for (Node n: g2.nodeMap.values()) {
			newG.nodeMap.put(n.getName(), n.clone());
			newG.considerableNodeMap.put(n.getName(), n.clone());
		}

		// Merge edges
		Set<Edge> edgesToMerge = new HashSet<Edge>();
		edgesToMerge.addAll(g1.edgeList);
		edgesToMerge.addAll(g2.edgeList);

		for (Edge e : edgesToMerge) {
			Edge newE = new Edge(e.getIntersect());
			Node fromNode = newG.nodeMap.get(e.getFromNode().getName());
			if(fromNode == null){
				System.out.println(e.toString());//debug
			}
			newE.setFromNode(fromNode);
			Node toNode = newG.nodeMap.get(e.getToNode().getName());
			newE.setToNode(toNode);
			newG.edgeList.add(newE);
			fromNode.getOutgoingEdgeList().add(newE);
			toNode.getIncomingEdgeList().add(newE);
		}
		init.removeDanglingNodes(newG);
		return newG;
	}

	private boolean enoughOverlap(GraphIndividual i1, GraphIndividual i2, double overlapPercentage) {
		Set<String> overlap1 = new HashSet<String>();
		overlap1.addAll( i1.nodeMap.keySet() );
		overlap1.retainAll(i2.nodeMap.keySet());
		Set<String> overlap2 = new HashSet<String>();
		overlap2.addAll( i2.nodeMap.keySet() );
		overlap2.retainAll(i1.nodeMap.keySet());

		return ((double) overlap1.size())/i1.nodeMap.size() >= overlapPercentage &&
				((double) overlap2.size())/i2.nodeMap.size() >= overlapPercentage;
	}
}
