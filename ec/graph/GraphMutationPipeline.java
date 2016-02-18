package ec.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ec.BreedingPipeline;
import ec.EvolutionState;
import ec.Individual;
import ec.util.Parameter;

public class GraphMutationPipeline extends BreedingPipeline {

	@Override
	public Parameter defaultBase() {
		return new Parameter("graphmutationpipeline");
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
			//This is to access the population species
			GraphSpecies species = (GraphSpecies)(state.population.subpops[0].species);
			Object[] nodes = graph.nodeMap.values().toArray();

			// Select node from which to perform mutation
			Node selected = null;
			while (selected == null) {
				Node temp = (Node) nodes[init.random.nextInt( nodes.length )];
				if (!temp.getName().equals( "end" )) {
					selected = temp;
				}
			}

			if (selected.getName().equals( "start" )) {
				// Create an entirely new graph
				graph = species.createNewGraph( null, state, init.startNode.clone(), init.endNode.clone(), init.relevant );
			}
			else {

				// Find all nodes that should be removed
				Node newEnd   = init.endNode.clone();
				Set<Node> nodesToRemove = findNodesToRemove(selected);
				Set<Edge> edgesToRemove = new HashSet<Edge>();

				// Remove nodes and edges
				for (Node node : nodesToRemove) {
					graph.nodeMap.remove( node.getName() );
					graph.considerableNodeMap.remove( node.getName() );

					for (Edge e : node.getIncomingEdgeList()) {
						edgesToRemove.add( e );
						e.getFromNode().getOutgoingEdgeList().remove( e );
					}
					for (Edge e : node.getOutgoingEdgeList()) {
						edgesToRemove.add( e );
						e.getToNode().getIncomingEdgeList().remove( e );
					}
				}

				for (Edge edge : edgesToRemove) {
					graph.edgeList.remove( edge );
					graph.considerableEdgeList.remove( edge );
				}


				// Create data structures
				Set<Node> unused = new HashSet<Node>(init.relevant);
				Set<Node> relevant = init.relevant;
				Set<String> currentEndInputs = new HashSet<String>();
				Set<Node> seenNodes = new HashSet<Node>();
				List<Node> candidateList = new ArrayList<Node>();

				for (Node node: graph.nodeMap.values()) {
					unused.remove( node );
					seenNodes.add( node );
				}

				// Must add all nodes as seen before adding candidate list entries
				for (Node node: graph.nodeMap.values()) {
					if (!node.getName().equals( "end" ))
						species.addToCandidateList( node, seenNodes, relevant, candidateList, init);
				}

				// Update currentEndInputs
				for (Node node : graph.nodeMap.values()) {
					for (String o : node.getOutputs()) {
						currentEndInputs.addAll(init.taxonomyMap.get(o).endNodeInputs);
					}
				}


				Collections.shuffle(candidateList, init.random);
				Map<String,Edge> connections = new HashMap<String,Edge>();
				graph.unused = unused;

				// Continue constructing graph
				species.finishConstructingGraph( currentEndInputs, newEnd, candidateList, connections, init,
						graph, null, seenNodes, relevant );

			}
			graph.evaluated=false;
			init.countGraphElements( graph );
		}
		return n;
	}

	private Set<Node> findNodesToRemove(Node selected) {
		Set<Node> nodes = new HashSet<Node>();
		_findNodesToRemove(selected, nodes);
		return nodes;

	}

	private void _findNodesToRemove(Node current, Set<Node> nodes) {
		nodes.add( current );
		for (Edge e: current.getOutgoingEdgeList()) {
			_findNodesToRemove(e.getToNode(), nodes);
		}
	}
}
