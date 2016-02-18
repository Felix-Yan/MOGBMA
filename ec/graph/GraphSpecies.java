package ec.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Map.Entry;
import java.util.Set;

import ec.EvolutionState;
import ec.Fitness;
import ec.Individual;
import ec.Species;
import ec.util.Parameter;

/**
 * This constructs GraphIndividuals.
 * @author yanlong
 *
 */
public class GraphSpecies extends Species {
	//int count;//debug
	@Override
	public Parameter defaultBase() {
		return new Parameter("graphspecies");
	}

	@Override
	public Individual newIndividual(EvolutionState state, int thread) {
		GraphInitializer init = (GraphInitializer) state.initializer;
		GraphIndividual graph = createNewGraph(null, state, init.startNode.clone(), init.endNode.clone(), init.relevant);
		//init.calculateMultiplicativeNormalisationBounds(graph.considerableNodeMap.values());

		return graph;
	}

	public GraphIndividual createNewGraph(GraphIndividual mergedGraph, EvolutionState state, Node start, Node end, Set<Node> relevant) {
		GraphInitializer init = (GraphInitializer) state.initializer;
		Set<Node> unused = new HashSet<Node>(init.relevant);

		GraphIndividual newGraph = new GraphIndividual(unused);

		Set<String> currentEndInputs = new HashSet<String>();
		Map<String,Edge> connections = new HashMap<String,Edge>();

		// Connect start node
		connectCandidateToGraphByInputs(start, connections, newGraph, currentEndInputs, init);

		Set<Node> seenNodes = new HashSet<Node>();
		//Set<Node> relevant = init.relevant;
		List<Node> candidateList = new ArrayList<Node>();

		if (mergedGraph != null)
			addToCandidateListFromEdges(start, mergedGraph, seenNodes, candidateList);
		else
			addToCandidateList(start, seenNodes, relevant, candidateList, init);

		Collections.shuffle(candidateList, init.random);

		finishConstructingGraph(currentEndInputs, end, candidateList, connections, init, newGraph, mergedGraph, seenNodes, relevant);

		//each individual should have initialized species objective function
		newGraph.fitness = (Fitness)(f_prototype.clone());
		
		return newGraph;
	}

	public void finishConstructingGraph(Set<String> currentEndInputs, Node end, List<Node> candidateList, Map<String,Edge> connections,
			GraphInitializer init, GraphIndividual newGraph, GraphIndividual mergedGraph, Set<Node> seenNodes, Set<Node> relevant) {

		// While end cannot be connected to graph
		while(!checkCandidateNodeSatisfied(init, connections, newGraph, end, end.getInputs(), null)){
			connections.clear();

			// Select node
			int index;

			candidateLoop:
				for (index = 0; index < candidateList.size(); index++) {
					Node candidate = candidateList.get(index).clone();
					// For all of the candidate inputs, check that there is a service already in the graph
					// that can satisfy it

					if (!checkCandidateNodeSatisfied(init, connections, newGraph, candidate, candidate.getInputs(), null)) {
						connections.clear();
						continue candidateLoop;
					}

					// Connect candidate to graph, adding its reachable services to the candidate list
					connectCandidateToGraphByInputs(candidate, connections, newGraph, currentEndInputs, init);
					connections.clear();

					if (mergedGraph != null)
						addToCandidateListFromEdges(candidate, mergedGraph, seenNodes, candidateList);
					else
						addToCandidateList(candidate, seenNodes, relevant, candidateList, init);

					break;
				}

			candidateList.remove(index);
			Collections.shuffle(candidateList, init.random);
		}

		connectCandidateToGraphByInputs(end, connections, newGraph, currentEndInputs, init);
		connections.clear();
		init.removeDanglingNodes(newGraph);
	}

	private boolean checkCandidateNodeSatisfied(GraphInitializer init,
			Map<String, Edge> connections, GraphIndividual newGraph,
			Node candidate, Set<String> candInputs, Set<Node> fromNodes) {

		Set<String> candidateInputs = new HashSet<String>(candInputs);
		Set<String> startIntersect = new HashSet<String>();

		// Check if the start node should be considered
		Node start = newGraph.nodeMap.get("start");

		if (fromNodes == null || fromNodes.contains(start)) {
			for(String output : start.getOutputs()) {
				Set<String> inputVals = init.taxonomyMap.get(output).servicesWithInput.get(candidate);
				if (inputVals != null) {
					candidateInputs.removeAll(inputVals);
					startIntersect.addAll(inputVals);
				}
			}

			if (!startIntersect.isEmpty()) {
				Edge startEdge = new Edge(startIntersect);
				startEdge.setFromNode(start);
				startEdge.setToNode(candidate);
				connections.put(start.getName(), startEdge);
			}
		}


		for (String input : candidateInputs) {
			boolean found = false;
			for (Node s : init.taxonomyMap.get(input).servicesWithOutput) {
				if (fromNodes == null || fromNodes.contains(s)) {
					if (newGraph.nodeMap.containsKey(s.getName())) {
						Set<String> intersect = new HashSet<String>();
						intersect.add(input);

						Edge mapEdge = connections.get(s.getName());
						if (mapEdge == null) {
							Edge e = new Edge(intersect);
							e.setFromNode(newGraph.nodeMap.get(s.getName()));
							e.setToNode(candidate);
							connections.put(e.getFromNode().getName(), e);
						} else
							mapEdge.getIntersect().addAll(intersect);

						found = true;
						break;
					}
				}
			}
			// If that input cannot be satisfied, move on to another candidate
			// node to connect
			if (!found) {
				// Move on to another candidate
				return false;
			}
		}
		return true;
	}

	private void addToCandidateListFromEdges (Node n, GraphIndividual mergedGraph, Set<Node> seenNode, List<Node> candidateList) {
		seenNode.add(n);

		Node original = mergedGraph.nodeMap.get(n.getName());

		for (Edge e : original.getOutgoingEdgeList()) {
			// Add servicesWithInput from taxonomy node as potential candidates to be connected
			Node current = e.getToNode();
			if (!seenNode.contains(current)) {
				candidateList.add(current);
				seenNode.add(current);
			}
		}
	}

	public void connectCandidateToGraphByInputs(Node candidate, Map<String,Edge> connections, GraphIndividual graph, Set<String> currentEndInputs, GraphInitializer init) {

		graph.nodeMap.put(candidate.getName(), candidate);
		graph.considerableNodeMap.put(candidate.getName(), candidate);
		graph.edgeList.addAll(connections.values());
		graph.considerableEdgeList.addAll(connections.values());
		candidate.getIncomingEdgeList().addAll(connections.values());

		for (Edge e : connections.values()) {
			Node fromNode = graph.nodeMap.get(e.getFromNode().getName());
			fromNode.getOutgoingEdgeList().add(e);
		}
		for (String o : candidate.getOutputs()) {
			currentEndInputs.addAll(init.taxonomyMap.get(o).endNodeInputs);
		}
		graph.unused.remove(candidate);
	}

	public void appendCandidateToGraphByInputs(Node candidate, Map<String,Edge> connections, GraphIndividual graph) {
		graph.nodeMap.put(candidate.getName(), candidate);
		graph.edgeList.addAll(connections.values());
		candidate.getIncomingEdgeList().addAll(connections.values());

		for (Edge e : connections.values()) {
			Node fromNode = graph.nodeMap.get(e.getFromNode().getName());
			fromNode.getOutgoingEdgeList().add(e);
		}
		graph.unused.remove(candidate);
	}

	public void appendCandidateToGraphByOutputs(Node candidate, Map<String,Edge> connections, GraphIndividual graph) {
		graph.nodeMap.put(candidate.getName(), candidate);
		graph.edgeList.addAll(connections.values());
		candidate.getOutgoingEdgeList().addAll(connections.values());

		for (Edge e : connections.values()) {
			Node toNode = graph.nodeMap.get(e.getToNode().getName());
			toNode.getIncomingEdgeList().add(e);
		}
		graph.unused.remove(candidate);
	}

	public void addToCandidateList(Node n, Set<Node> seenNode, Set<Node> relevant, List<Node> candidateList, GraphInitializer init) {
		seenNode.add(n);
		List<TaxonomyNode> taxonomyOutputs;
		if (n.getName().equals("start")) {
			taxonomyOutputs = new ArrayList<TaxonomyNode>();
			for (String outputVal : n.getOutputs()) {
				taxonomyOutputs.add(init.taxonomyMap.get(outputVal));
			}
		}
		else
			taxonomyOutputs = init.serviceMap.get(n.getName()).getTaxonomyOutputs();

		for (TaxonomyNode t : taxonomyOutputs) {
			// Add servicesWithInput from taxonomy node as potential candidates to be connected
			for (Node current : t.servicesWithInput.keySet()) {
				if (!seenNode.contains(current) && relevant.contains(current)) {
					candidateList.add(current);
					seenNode.add(current);
				}
			}
		}
	}

	public boolean checkNewGraphNode(GraphInitializer init, GraphIndividual graph, Node n, String input, Map<String,Edge> connections, Set<Node> fromNodes) {
		boolean foundMatch = false;

		// Check if start node should be considered as a candidate
		Node start = graph.nodeMap.get("start");
		if(fromNodes.contains(start)) {

			Set<String> startIntersect = new HashSet<String>();

			for(String output : start.getOutputs()) {
				Set<String> inputVals = init.taxonomyMap.get(output).servicesWithInput.get(n);
				if (inputVals != null) {
					//candidateInputs.removeAll(inputVals);
					startIntersect.addAll(inputVals);
				}
			}

			if (!startIntersect.isEmpty()) {
				Edge startEdge = new Edge(startIntersect);
				startEdge.setFromNode(start);
				startEdge.setToNode(n);
				connections.put(start.getName(), startEdge);
				return true;
			}
		}

		for (Node candidate : init.taxonomyMap.get(input).servicesWithOutput){
			if (fromNodes.contains(candidate)) {

				Node graphC = graph.nodeMap.get(candidate.getName());
				Set<String> intersect = new HashSet<String>();
				intersect.add(input);

				Edge mapEdge = connections.get(graphC.getName());
				foundMatch = true;

				if (mapEdge == null) {
					Edge e = new Edge(intersect);
					e.setFromNode(graph.nodeMap.get(graphC.getName()));
					e.setToNode(n);
					connections.put(e.getFromNode().getName(), e);
				}
				else
					mapEdge.getIntersect().addAll(intersect);

				break;
			}
		}
		return foundMatch;
	}

	/**
	 * This connects a subgraph to the main graph with matched input and outputs
	 * @param init
	 * @param graph
	 * @param subgraph
	 * @param disconnectedInput
	 * @param disconnectedOutput
	 */
	public void fitMutatedSubgraph(GraphInitializer init, GraphIndividual graph, GraphIndividual subgraph, Map<Node, Set<String>> disconnectedInput, Set<Node> disconnectedOutput){

		// Add subgraph to main graph
		Map<Node, Set<String>> firstSubgraphLayer = new HashMap<Node, Set<String>>();//all the subgraph nodes connected to start
		Set<Node> lastSubgraphLayer = new HashSet<Node>();//all the subgraph nodes connected to end

		/*
		 * add all nodes that are neither start nor end to the nodeMaps of the graph
		 */
		for (Node n : subgraph.nodeMap.values()) {
			if (!n.getName().equals( "start" ) && !n.getName().equals( "end" )){
				Node newN = n.clone();
				graph.nodeMap.put( newN.getName(), newN );
				graph.considerableNodeMap.put( newN.getName(), newN );
			}
		}

		for (Node n : subgraph.nodeMap.values()) {
			//add all the subgraph nodes connected with "end" to the last layer
			if(n.getName().equals( "end" )) {
				for (Edge e : n.getIncomingEdgeList()) {
					lastSubgraphLayer.add(e.getFromNode());
				}
			}
			else{
				for (Edge e : n.getIncomingEdgeList()){
					//add all the subgraph nodes connected with "start" to the first layer
					if (e.getFromNode().getName().equals( "start" )) {
						firstSubgraphLayer.put(n, e.getIntersect());
					}
					//add all other edges to the graph
					else {
						addNewGraphEdge(e, graph);
					}
				}
			}
		}

		// Match first subgraph layer with nodes from main graph whose output has been disconnected
		Map<String,Edge> connections = new HashMap<String,Edge>();

		for (Entry <Node, Set<String>> entry: firstSubgraphLayer.entrySet()) {
			connections.clear();
			Node n = graph.nodeMap.get( entry.getKey().getName() );

			// Find all input connections
			if (!checkCandidateNodeSatisfied(init, connections, graph, n, entry.getValue(), disconnectedOutput))
				throw new RuntimeException("Cannot satisfy subgraph outputs.");

			// Connect it to graph
			for (Edge e : connections.values()) {
				graph.edgeList.add(e);
				graph.considerableEdgeList.add(e);
				e.getFromNode().getOutgoingEdgeList().add(e);
				e.getToNode().getIncomingEdgeList().add(e);
			}
		}

		// Match last subgraph layer with nodes from main graph whose input has been disconnected
		for (Entry<Node, Set<String>> entry : disconnectedInput.entrySet()) {
			connections.clear();

			// Find all input connections
			if (!checkCandidateNodeSatisfied(init, connections, graph, entry.getKey(), entry.getValue(), lastSubgraphLayer))
				throw new RuntimeException("Cannot satisfy subgraph outputs.");

			// Connect it to graph
			for (Edge e : connections.values()) {
				graph.edgeList.add(e);
				graph.considerableEdgeList.add(e);
				e.getFromNode().getOutgoingEdgeList().add(e);
				e.getToNode().getIncomingEdgeList().add(e);
			}
		}
	}

	private void addNewGraphEdge(Edge e, GraphIndividual destGraph){
		Edge newE = new Edge(e.getIntersect());
		newE.setFromNode( destGraph.nodeMap.get( e.getFromNode().getName() ) );
		newE.setToNode( destGraph.nodeMap.get( e.getToNode().getName() ) );

		destGraph.nodeMap.get( e.getFromNode().getName() ).getOutgoingEdgeList().add(newE);
		destGraph.nodeMap.get( e.getToNode().getName() ).getIncomingEdgeList().add(newE);

		destGraph.edgeList.add(newE);
		destGraph.considerableEdgeList.add(newE);
	}

	public Set<Node> selectNodes(Node root, int numNodes) {

		Set<Node> nodes = new HashSet<Node>();

		Queue<Node> queue = new LinkedList<Node>();
		queue.offer( root );

		for (int i = 0; i < numNodes; i++) {
			Node current = queue.poll();

			if(current == null || current.getName().equals( "end" )){
				break;
			}
			else {
				nodes.add(current);
				for(Edge e : current.getOutgoingEdgeList()){

					// Check that node is entirely fulfilled by nodes already selected
					boolean isInternal = true;
					for (Edge incomingList : e.getToNode().getIncomingEdgeList()) {
						if (!nodes.contains(incomingList.getFromNode())) {
							isInternal = false;
							break;
						}
					}

					if (isInternal) {
						queue.offer( e.getToNode() );
					}
				}
			}
		}
		return nodes;
	}

	//==========================================================================================================================
	//                                                 Debugging Routines
	//==========================================================================================================================

	public boolean structureValidator1( GraphIndividual graph ) {
		for ( Edge e : graph.edgeList ) {
			//Node fromNode = e.getFromNode();
			Node fromNode = graph.nodeMap.get( e.getFromNode().getName());

			boolean isContained = false;
			for ( Edge outEdge : fromNode.getOutgoingEdgeList() ) {
				if ( e == outEdge ) {
					isContained = true;
					break;
				}
			}

			if ( !isContained ) {
				System.out.println( "Outgoing edge for node " + fromNode.getName() + " not detected." );
				return false;
			}

			//Node toNode = e.getToNode();
			Node toNode = graph.nodeMap.get( e.getToNode().getName());

			isContained = false;
			for ( Edge inEdge : toNode.getIncomingEdgeList() ) {
				if ( e == inEdge ) {
					isContained = true;
					break;
				}
			}

			if ( !isContained ) {
				System.out.println( "Incoming edge for node " + toNode.getName() + " not detected." );
				return false;
			}
		}
		//System.out.println("----------------------------------------------1");
		return true;
	}

	public boolean structureValidator2( GraphIndividual graph ) {
		for ( Edge e : graph.considerableEdgeList ) {
			Node fromNode = graph.considerableNodeMap.get( e.getFromNode().getName());

			boolean isContained = false;
			for ( Edge outEdge : fromNode.getOutgoingEdgeList() ) {
				if ( e == outEdge ) {
					isContained = true;
					break;
				}
			}

			if ( !isContained ) {
				System.out.println( "Considerable: Outgoing edge for node " + fromNode.getName() + " not detected." );
				return false;
			}

			Node toNode = graph.considerableNodeMap.get( e.getToNode().getName());

			isContained = false;
			for ( Edge inEdge : toNode.getIncomingEdgeList() ) {
				if ( e == inEdge ) {
					isContained = true;
					break;
				}
			}

			if ( !isContained ) {
				System.out.println( "Considerable: Incoming edge for node " + toNode.getName() + " not detected." );
				return false;
			}
		}
		//System.out.println("----------------------------------------------2");
		return true;
	}

	/**
	 * Checks whether there are edges beginning and ending at the same node.
	 *
	 * @param graph
	 */
	public boolean structureValidator3( GraphIndividual graph ) {
		for (Edge e : graph.edgeList) {
			if (e.getFromNode().getName().equals(e.getToNode().getName())) {
				System.out.println(String.format("Edge '%s' makes a loop.", e));
				return false;
			}
		}
		//System.out.println("----------------------------------------------3");
		return true;
	}

	/**
	 * Checks whether there are any duplicated edges
	 * @param graph
	 */
	public boolean structureValidator4( GraphIndividual graph ) {
		for (Edge e1 : graph.edgeList) {
			for (Edge e2 : graph.edgeList) {
				if (e1 != e2 && e1.getFromNode().getName().equals(e2.getFromNode().getName()) && e1.getToNode().getName().equals(e2.getToNode().getName())) {
					System.out.println(String.format("Edge '%s' has a duplicate.", e1));
					return false;
				}
			}
		}
		//System.out.println("----------------------------------------------4");
		return true;
	}

	/**
	 * Checks if the total number of inputs provided by the incoming edges matches the total number
	 * of inputs required by a given node, across all nodes in the graph.
	 *
	 * @param graph
	 * @return
	 */
	public boolean structureValidator5( GraphIndividual graph ) {
		for (Node n : graph.nodeMap.values()) {
			Set<String> incomingValues = new HashSet<String>();
			for (Edge e : n.getIncomingEdgeList()) {
				incomingValues.addAll(e.getIntersect());
			}

			if (incomingValues.size() != n.getInputs().size()) {
				System.out.println(String.format("Not all inputs of node '%s' are being satisfied.", n));
				return false;
			}
		}
		//System.out.println("----------------------------------------------5");
		return true;
	}

	/**
	 * Checks if our graph has start and end nodes.
	 *
	 * @param graph
	 * @return
	 */
	public boolean structureValidator6( GraphIndividual graph ) {
		if (!graph.nodeMap.containsKey("start")) {
			System.out.println(String.format("The graph doesn't have a start node."));
			return false;
		}
		else if (!graph.nodeMap.containsKey("end")) {
			System.out.println(String.format("The graph doesn't have an end node."));
			return false;
		}
		return true;
	}

	public boolean structureValidator7( GraphIndividual graph ) {
		for (Node n : graph.nodeMap.values()) {

		}
		return true;
	}
}
