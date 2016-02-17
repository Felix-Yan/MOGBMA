package ec.graph;

import java.util.ArrayList;
import java.util.Collection;
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
/**
 * This reserves the 1-1 then 2-1 version of memetic algorithm.
 * This is version 4.
 * @author yanlong
 *
 */
public class GraphMemeticPipeline4 extends BreedingPipeline {
	GraphIndividual currentGraph;
	Node newSelection;
	Set<Edge> newDomain;
	int count;//debug

	@Override
	public Parameter defaultBase() {
		return new Parameter("graphmemeticpipeline");
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
			GraphSpecies species = (GraphSpecies) graph.species;
			Object[] nodes = graph.nodeMap.values().toArray();
			// Select node from which to perform mutation
			Node selected = null;
			while (selected == null) {
				Node temp = (Node) nodes[init.random.nextInt( nodes.length )];
				//Do not allow mutations for start or end node
				if (!temp.getName().equals( "end" ) && !temp.getName().equals( "start" )) {
					selected = temp;
					newSelection = temp;
				}
			}
			double bestFitness = 0;
			double currentBestFitness = 0;
			//reset currentGraph and newDomain
			currentGraph = new GraphIndividual();
			newDomain = new HashSet<Edge>();
			graph.copyTo(currentGraph);
			selected = currentGraph.nodeMap.get(selected.getName());//change the reference to the currentGraph

			// Find all nodes that should be locally searched and possibly replaced
			Set<Node> nodesToReplace = findNodesToRemove(selected);
			do{
				selected = currentGraph.nodeMap.get(newSelection.getName());//update the selected node in each loop
				if(selected == null){
					throw new NullPointerException("The node selected should not be null");
				}
				bestFitness = currentBestFitness;
				currentBestFitness = findFitness(nodesToReplace, init, state, currentGraph, subpopulation, thread,selected);
			}while(currentBestFitness > bestFitness);
			currentGraph.evaluated = false;
			selected = currentGraph.nodeMap.get(newSelection.getName());//update the selected node if it has been replaced
			Set<Edge> edgesMemetic = findEdges(selected);//selected is a node from "currentGraph"
			do{
				if(newDomain != null) edgesMemetic = newDomain;
				selected = currentGraph.nodeMap.get(newSelection.getName());//update the selected node if it has been replaced
				if(selected == null){
					throw new NullPointerException("The node selected should not be null");
				}
				bestFitness = currentBestFitness;
				currentBestFitness = execute2for1(edgesMemetic, init, state, currentGraph, subpopulation, thread, selected);
			}while(currentBestFitness > bestFitness);
			inds[q] = currentGraph;
			//debug
			/*if(!currentGraph.validation()){
				throw new IllegalArgumentException("Graph's edges and nodes are not consistent");
			}
			System.out.println(currentGraph.toString());*/
			//debug
			//System.out.println(count+"Memetic");
			/*if(count == 22){
				System.out.println("time to debug");
			}*/
			count++;

		}
		return n;
	}

	/*
	 * This returns a fitness value after performing 2-1 node local optimization.
	 */
	private double execute2for1(Set<Edge> domain, GraphInitializer init, EvolutionState state,
			GraphIndividual graph, int subpopulation, int thread, Node selected){
		((GraphEvol)state.evaluator.p_problem).evaluate(state, graph, subpopulation, thread);//graph here is the "currentGraph"
		graph.evaluated = false;
		double currentFitness = graph.fitness.fitness();
		GraphIndividual bestGraph = new GraphIndividual();
		Node newMember = null;//The new node added in the subgraph
		Edge replaced = null;//The old edge replaced in the subgraph
		for (Edge edge : domain) {
			Set<Node> neighbours = find2for1Candidates(edge,init);
			if(neighbours.size() != 0){
				System.out.println("neighbours: "+neighbours.size()+"===========================================");//debug
			}
			for(Node neighbour: neighbours){
				GraphIndividual innerGraph = new GraphIndividual();
				graph.copyTo(innerGraph);
				replaceNode2for1(edge, neighbour, innerGraph, init, selected);
				((GraphEvol)state.evaluator.p_problem).evaluate(state, innerGraph, subpopulation, thread);
				double fitness = innerGraph.fitness.fitness();
				if(fitness > currentFitness){
					currentFitness = fitness;
					innerGraph.copyTo(bestGraph);
					replaced = edge;
					newMember = neighbour;
				}
			}
		}
		if(replaced!=null){
			bestGraph.copyTo(currentGraph);
			//update the root node if it has been replaced
			/*if(replaced.getFromNode().getName().equals(selected.getName())){
				newSelection = newMember;
				selected = newMember;
			}*/
			newDomain = findEdges(currentGraph.nodeMap.get(selected.getName()));
			System.out.println("edges updated");//debug
			//System.out.println("replaced: "+replaced.getName());
			//System.out.println("added: "+newMember.getName());
		}
		return currentFitness;
	}

	/*
	 * This returns the best new fitness of the graph after a local search
	 */
	private double findFitness(Set<Node> domain, GraphInitializer init, EvolutionState state,
			GraphIndividual graph, int subpopulation, int thread, Node selected){
		((GraphEvol)state.evaluator.p_problem).evaluate(state, graph, subpopulation, thread);
		graph.evaluated = false;
		double currentFitness = graph.fitness.fitness();
		GraphIndividual bestGraph = new GraphIndividual();
		Node newMember = null;//The new node added in the subgraph
		Node replaced = null;//The old node replaced by the new node in the subgraph
		//debug
		//		System.out.println("nodes to replace has size"+" "+domain.size());

		for (Node node : domain) {
			Set<Node> neighbours = findNeighbourNodes(node, init);
			for(Node neighbour: neighbours){
				GraphIndividual innerGraph = new GraphIndividual();
				graph.copyTo(innerGraph);
				replaceNode(node, neighbour, innerGraph, init);
				((GraphEvol)state.evaluator.p_problem).evaluate(state, innerGraph, subpopulation, thread);
				double fitness = innerGraph.fitness.fitness();
				if(fitness > currentFitness){
					currentFitness = fitness;
					innerGraph.copyTo(bestGraph);
					replaced = node;
					newMember = neighbour;
				}
			}

		}
		if(replaced!=null){
			bestGraph.copyTo(currentGraph);;
			domain.remove(replaced);
			domain.add(newMember);
			if(replaced.getName().equals(selected.getName())){
				newSelection = newMember;
				//System.out.println("Root changed");//debug
			}
			//System.out.println("replaced: "+replaced.getName());
			//System.out.println("added: "+newMember.getName());
		}
		return currentFitness;
	}

	/*
	 * Replace the node with its neighbour in the graph.
	 */
	private void replaceNode(Node node, Node neighbour, GraphIndividual newGraph, GraphInitializer init){
		//do not replace end node
		if(node.getName().equals("end")) return;
		//do not replace a node by itself
		if(node.getName().equals(neighbour.getName())) return;
		//do not add the neighbour if the neighbour has already been in the graph. Do not allow duplicates
		if(newGraph.nodeMap.get(neighbour.getName()) != null) return;
		//this is to obtain the node with the name from the current graph
		Node graphNode = newGraph.nodeMap.get(node.getName());
		//debug
		if(graphNode == null){
			System.out.println("The nonexistent selected node is "+node.getName());
			throw new NullPointerException("The node to be replaced does not exist in graph");
		}
		//debug
		/*if(graphNode.getName().equals("serv840365223")){
			System.out.println("This node will be missing");
		}*/

		Set<Edge> outgoingEdges = new HashSet<Edge>();
		Set<Edge> incomingEdges = new HashSet<Edge>();

		//add the neighbour node to the graph
		newGraph.nodeMap.put(neighbour.getName(), neighbour);
		newGraph.considerableNodeMap.put(neighbour.getName(), neighbour);

		//remove incoming edges of the replaced node
		for (Edge e : graphNode.getIncomingEdgeList()) {
			Edge newEdge = e.cloneEdge(newGraph.nodeMap);
			incomingEdges.add( newEdge );
			e.getFromNode().getOutgoingEdgeList().remove( e );
			newGraph.edgeList.remove( e );
			newGraph.considerableEdgeList.remove( e );
		}

		//remove outgoingEdges to the neighbour node
		for (Edge e : graphNode.getOutgoingEdgeList()) {
			Edge newEdge = e.cloneEdge(newGraph.nodeMap);
			outgoingEdges.add( newEdge );
			e.getToNode().getIncomingEdgeList().remove( e );
			newGraph.edgeList.remove( e );
			newGraph.considerableEdgeList.remove( e );
		}

		neighbour.getOutgoingEdgeList().clear();//this removes all other unnecessary inherited edges
		neighbour.getIncomingEdgeList().clear();

		//give outgoingEdges to the neighbour node
		for(Edge e: outgoingEdges){
			//e.setFromNode(newGraph.nodeMap.get(neighbour.getName()));
			e.setFromNode(neighbour);
			neighbour.getOutgoingEdgeList().add(e);
			e.getToNode().getIncomingEdgeList().add(e);
			newGraph.edgeList.add(e);
			newGraph.considerableEdgeList.add(e);
		}

		//give incomingEdges to the neighbour node
		for(Edge e: incomingEdges){
			Set<String> nodeInputs = neighbour.getInputs();
			Set<String> edgeInputs = e.getIntersect();
			//check if the edge is still useful for the neighbour
			if(init.isIntersection(edgeInputs, nodeInputs)){
				e.setToNode(newGraph.nodeMap.get(neighbour.getName()));
				neighbour.getIncomingEdgeList().add(e);
				e.getFromNode().getOutgoingEdgeList().add( e );
				newGraph.edgeList.add(e);
				newGraph.considerableEdgeList.add(e);
			}
		}
		init.removeDanglingNodes(newGraph);
		//remove the node to be replaced
		newGraph.nodeMap.remove( graphNode.getName() );
		newGraph.considerableNodeMap.remove( graphNode.getName() );
	}

	/*
	 * Replace 2 nodes with 1 node in the graph.
	 */
	private void replaceNode2for1(Edge selected, Node neighbour, GraphIndividual newGraph, GraphInitializer init, Node root){
		//this is to obtain the node with the name from the current graph
		Node fromNode = selected.getFromNode();
		Node toNode = selected.getToNode();
		Node newNeighbour = neighbour.clone();
		String newName = neighbour.getName();
		/*
		 * do not add the neighbour if the neighbour has already been in the graph.
		 * Unless the neighbour is one of the fromNode or toNode.
		 * Do not allow duplicates
		 */
		if(newGraph.nodeMap.get(newName) != null && !fromNode.getName().equals(newName)
				&& !toNode.getName().equals(newName)) return;

		Node graphFromNode = newGraph.nodeMap.get(fromNode.getName());
		Node graphToNode = newGraph.nodeMap.get(toNode.getName());
		if(graphFromNode == null || graphToNode == null){
			throw new NullPointerException("cannot find the edge in the graph");
		}
		Set<Edge> outgoingEdges = new HashSet<Edge>();
		List<Edge> incomingEdges = new ArrayList<Edge>();
		Set<Edge> incomingEdges2 = giveNewEdgeSet(graphToNode.getIncomingEdgeList(), newGraph);
		Set<Edge> incomingEdges1 = giveNewEdgeSet(graphFromNode.getIncomingEdgeList(), newGraph);
		Set <Edge> outgoingEdge1 = giveNewEdgeSet(graphToNode.getOutgoingEdgeList(), newGraph);
		Set <Edge> outgoingEdge2 = giveNewEdgeSet(graphFromNode.getOutgoingEdgeList(), newGraph);
		Set <Node> incomingNodes = new HashSet<Node>();//nodes that give incoming edges
		Set <Node> outgoingNodes = new HashSet<Node>();//nodes that receive outgoing edges
		outgoingEdges.addAll(outgoingEdge1);
		for(Edge e: outgoingEdges){
			outgoingNodes.add(e.getToNode());
		}
		//combine the outgoindEdges of both fromNode and toNode to be the desired outgoingEdges of a new node
		for(Edge e: outgoingEdge2){
			if(!e.getToNode().getName().equals(toNode.getName())){
				if(outgoingNodes.contains(e.getToNode())){
					mergeOutgoingEdges(outgoingEdges, e);
				}else{
					outgoingEdges.add(e);
				}
			}
		}
		//combine the incomingEdges of both fromNode and toNode to be the desired incomingEdges of a new node
		incomingEdges.addAll(incomingEdges1);
		for(Edge e: incomingEdges){
			incomingNodes.add(e.getFromNode());
		}
		for(Edge e: incomingEdges2){
			if(!e.getFromNode().getName().equals(fromNode.getName())){//edges not from the fromNode
				if(incomingNodes.contains(e.getFromNode())){
					mergeIncomingEdges(incomingEdges,e);
				}else{
					incomingEdges.add(e);
					//riskyEdges.add(e);
				}
			}
		}
		//remove incoming and outgoing edges of the replaced node
		removeIncomingEdges(graphFromNode,newGraph);
		removeIncomingEdges(graphToNode,newGraph);
		removeOutgoingEdges(graphFromNode,newGraph);
		removeOutgoingEdges(graphToNode,newGraph);
		//this removes all other unnecessary inherited edges
		newNeighbour.getOutgoingEdgeList().clear();
		newNeighbour.getIncomingEdgeList().clear();
		//give outgoingEdges to the neighbour node
		for(Edge e: outgoingEdges){
			e.setFromNode(newNeighbour);
			newNeighbour.getOutgoingEdgeList().add(e);
			e.getToNode().getIncomingEdgeList().add(e);
			newGraph.edgeList.add(e);
			newGraph.considerableEdgeList.add(e);
		}
		//add the neighbour node to the graph
		newGraph.nodeMap.put(newNeighbour.getName(), newNeighbour);
		newGraph.considerableNodeMap.put(newNeighbour.getName(), newNeighbour);
		Set<String> unsatisfiedInputs = new HashSet<String>(newNeighbour.getInputs());
		//give incomingEdges to the neighbour node
		for(Edge e: incomingEdges){
			Set<String> edgeInputs = e.getIntersect();
			//check if the edge is still useful for the neighbour
			if(init.isIntersection(edgeInputs, unsatisfiedInputs)){
				unsatisfiedInputs.removeAll(edgeInputs);//by doing this, redundant edges will not be added
				e.setToNode(newGraph.nodeMap.get(newNeighbour.getName()));
				newNeighbour.getIncomingEdgeList().add(e);
				e.getFromNode().getOutgoingEdgeList().add( e );
				newGraph.edgeList.add(e);
				newGraph.considerableEdgeList.add(e);
			}
		}
		//remove the 2 nodes to be replaced
		newGraph.nodeMap.remove( graphFromNode.getName() );
		newGraph.considerableNodeMap.remove( graphFromNode.getName() );
		newGraph.nodeMap.remove( graphToNode.getName() );
		newGraph.considerableNodeMap.remove( graphToNode.getName() );
		//add the neighbour node to the graph again in case it was one of the from or to nodes.
		newGraph.nodeMap.put(newNeighbour.getName(), newNeighbour);
		newGraph.considerableNodeMap.put(newNeighbour.getName(), newNeighbour);
		init.removeDanglingNodes(newGraph);

	}

	/*
	 * This merges the outgoing edges that will result in same from and to Nodes
	 */
	private void mergeOutgoingEdges(Set<Edge> outgoingEdges, Edge edge){
		Node toNode = edge.getToNode();
		for(Edge e: outgoingEdges){
			if(e.getToNode().getName().equals(toNode.getName())){
				e.addIntersects(edge.getIntersect());
				return;
			}
		}
		throw new IllegalArgumentException("the edges cannot be merged");
	}

	/*
	 * This merges the incoming edges that will result in same from and to Nodes
	 */
	private void mergeIncomingEdges(List<Edge> incomingEdges, Edge edge){
		Node fromNode = edge.getFromNode();
		for(Edge e: incomingEdges){
			if(e.getFromNode().getName().equals(fromNode.getName())){
				e.addIntersects(edge.getIntersect());
				return;
			}
		}
		throw new IllegalArgumentException("the edges cannot be merged");
	}

	/*
	 * This removes the incoming edges of a node in the graph.
	 */
	private void removeIncomingEdges(Node node, GraphIndividual newGraph){
		for (Edge e : node.getIncomingEdgeList()) {
			e.getFromNode().getOutgoingEdgeList().remove( e );
			newGraph.edgeList.remove( e );
			newGraph.considerableEdgeList.remove( e );
		}
	}

	/*
	 * This removes the outgoing edges of a node in the graph.
	 */
	private void removeOutgoingEdges(Node node, GraphIndividual newGraph){
		for (Edge e : node.getOutgoingEdgeList()) {
			e.getToNode().getIncomingEdgeList().remove( e );
			newGraph.edgeList.remove( e );
			newGraph.considerableEdgeList.remove( e );
		}
	}

	/*
	 * This gives a new set of edges with new references from the original edgelist
	 */
	private Set<Edge> giveNewEdgeSet(List<Edge> edges, GraphIndividual newGraph){
		Set<Edge> edgeset = new HashSet<Edge>();
		for(Edge e: edges){
			Edge newEdge = e.cloneEdge(newGraph.nodeMap);
			edgeset.add(newEdge);
		}
		return edgeset;
	}
	/*
	 * This finds all the neighbouring nodes of the selected node. The neighbours can substitute the selected node without
	 * losing any functionality.
	 */
	private Set<Node> findNeighbourNodes(Node selected, GraphInitializer init){
		List <Edge> outgoingEdge = selected.getOutgoingEdgeList();

		//use the selected node inputs as the possible neighbour inputs
		Set<String> inputs = selected.getInputs();
		Set<String> outputs = new HashSet<String>();

		//use all the outputs in the outgoing edges as the required neighbour outputs
		for(Edge e: outgoingEdge){
			outputs.addAll(e.getIntersect());
		}

		Set<Node> nodeWithOutput = new HashSet<Node>();
		//The following finds out all the nodes that satisfy all the outputs
		for(String output: outputs){
			if(nodeWithOutput.isEmpty()){
				nodeWithOutput = new HashSet<Node>(init.taxonomyMap.get(output).servicesWithOutput);
			}
			else{
				Set<Node> nodeWithOutput2 = new HashSet<Node>(init.taxonomyMap.get(output).servicesWithOutput);
				nodeWithOutput = findIntersection(nodeWithOutput, nodeWithOutput2);
				//if there is no intersection, return no neighbours
				if(nodeWithOutput.size() == 0){
					return new HashSet<Node>();
				}
			}
		}

		Set<Node> neighbours = new HashSet<Node>();
		neighbours.addAll(nodeWithOutput);
		//This checks that all the neighbours can be satisfied by the given inputs
		for(Node node: nodeWithOutput){
			Set<String> nodeInput = node.getInputs();
			if(!isSubset(nodeInput, inputs)){
				neighbours.remove(node);
			}
		}

		return neighbours;
	}

	/*
	 * This finds all the single nodes can be used to replace the given two neighbour nodes.
	 */
	private Set<Node> find2for1Candidates(Edge selected, GraphInitializer init){
		Node fromNode = selected.getFromNode();
		Node toNode = selected.getToNode();
		List <Edge> outgoingEdges = new ArrayList<Edge>();
		List <Edge> outgoingEdge1 = toNode.getOutgoingEdgeList();
		List <Edge> outgoingEdge2 = fromNode.getOutgoingEdgeList();
		outgoingEdges.addAll(outgoingEdge1);
		//combine the outgoindEdges of both fromNode and toNode to be the desired outgoingEdges of a new node
		for(Edge e: outgoingEdge2){
			if(!e.getToNode().getName().equals(toNode.getName())){
				outgoingEdges.add(e);
			}
		}
		Set<Node> outgoingNodes = new HashSet<Node>();
		for(Edge e: outgoingEdges){
			outgoingNodes.add(e.getToNode());
		}
		//use the fromNode inputs as the possible neighbour inputs
		Set<String> inputs = new HashSet<String>();
		Set<String> inputs1 = fromNode.getInputs();
		inputs.addAll(inputs1);
		//use toNode inputs that are not from the fromNode as neighbour inputs
		List<Edge> incomingToNode = toNode.getIncomingEdgeList();
		for(Edge e: incomingToNode){
			if(!e.getFromNode().getName().equals(fromNode.getName())){
				if(!outgoingNodes.contains(e.getFromNode())){//eliminate possible cycles
					inputs.addAll(e.getIntersect());
				}
			}
		}
		Set<String> outputs = new HashSet<String>();

		//use all the outputs in the outgoing edges as the required neighbour outputs
		for(Edge e: outgoingEdges){
			outputs.addAll(e.getIntersect());
		}

		Set<Node> nodeWithOutput = new HashSet<Node>();
		//The following finds out all the nodes that satisfy all the outputs
		for(String output: outputs){
			if(nodeWithOutput.isEmpty()){
				nodeWithOutput = new HashSet<Node>(init.taxonomyMap.get(output).servicesWithOutput);
			}
			else{
				Set<Node> nodeWithOutput2 = new HashSet<Node>(init.taxonomyMap.get(output).servicesWithOutput);
				nodeWithOutput = findIntersection(nodeWithOutput, nodeWithOutput2);
				//if there is no intersection, return no neighbours
				if(nodeWithOutput.size() == 0){
					return new HashSet<Node>();
				}
			}
		}
		Set<Node> neighbours = new HashSet<Node>();
		neighbours.addAll(nodeWithOutput);
		//This checks that all the neighbours can be satisfied by the given inputs
		for(Node node: nodeWithOutput){
			Set<String> nodeInput = node.getInputs();
			if(!isSubset(nodeInput, inputs)){
				neighbours.remove(node);
			}
		}
		return neighbours;
	}


	/*
	 * The following checks that the given set1 is a subset of set2.
	 */
	private boolean isSubset(Set<String> set1, Set<String> set2){
		for(String s: set1){
			if(!set2.contains(s)){
				return false;
			}
		}
		return true;
	}

	/*
	 * This gives the intersection of two set of nodes
	 */
	private Set<Node> findIntersection(Set<Node> set1, Set<Node> set2){
		Set<Node> intersection = new HashSet<Node>();
		for(Node n: set1){
			if(set2.contains(n)){
				intersection.add(n);
			}
		}
		return intersection;
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

	//Not sure if this is right. Might as well use Alex's version.
	/*private Set<Edge> findEdges(Node selected, Set<Edge> edges){
		for(Edge e: selected.getOutgoingEdgeList()){
			edges.add(e);
			Node node = e.getToNode();
			edges.addAll(findEdges(node, edges));
		}
		return edges;
	}*/

	/*
	 * This finds out all the edges involved in the memetic operator from the selected node.
	 */
	private Set<Edge> findEdges(Node selected){
		Set<Edge> edges = new HashSet<Edge>();
		recFindEdges(selected, edges);
		return edges;
	}

	/*
	 * This gathers all the required edges recursively.
	 */
	private void recFindEdges(Node current, Set<Edge> edges){
		for(Edge e: current.getOutgoingEdgeList()){
			Node toNode = e.getToNode();
			if(!toNode.getName().equals("end")){
				edges.add(e);
			}
			recFindEdges(toNode,edges);
		}
	}
}
