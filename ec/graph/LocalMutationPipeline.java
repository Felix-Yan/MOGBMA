package ec.graph;

import java.util.ArrayList;
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
 *
 * @author Alex
 */
public class LocalMutationPipeline extends BreedingPipeline {
    public static int counter = 0;

    @Override
    public Parameter defaultBase() {
        return new Parameter("localmutationpipeline");
    }

    @Override
    public int numSources() {
        return 1;
    }

    @Override
    public int produce( int min, int max, int start, int subpopulation,
            Individual[] inds, EvolutionState state, int thread) {
        counter++;

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

            List<Node> nodeList = new ArrayList<Node>(graph.nodeMap.values());
            nodeList.remove(graph.nodeMap.get("start"));
            nodeList.remove(graph.nodeMap.get("end"));

            // Randomly select node from which to perform mutation (excluding start and end nodes)
        	int index = init.random.nextInt(nodeList.size());
            Node selected = nodeList.get(index);

            // Select the additional nodes that will be involved in the mutation
            double[] mockQos = new double[4];
            mockQos[GraphInitializer.TIME] = 0;
            mockQos[GraphInitializer.COST] = 0;
            mockQos[GraphInitializer.AVAILABILITY] = 1;
            mockQos[GraphInitializer.RELIABILITY] = 1;

            Set<String> taskInput = new HashSet<String>();
            Set<String> taskOutput = new HashSet<String>();

            Node localStartNode = new Node("start", mockQos, new HashSet<String>(), taskInput);
            Node localEndNode = new Node("end", mockQos, taskOutput ,new HashSet<String>());
            Map<Node, Set<String>> disconnectedInput = new HashMap<Node, Set<String>>();
            Set<Node> disconnectedOutput = new HashSet<Node>();


            removeMutationNodes(species, init.numNodesMutation, selected, graph, taskInput, taskOutput, disconnectedInput, disconnectedOutput);

            // Generate the new subgraph
            Set<Node> nodesToConsider = new HashSet<Node>(init.relevant);
            nodesToConsider.removeAll(graph.nodeMap.values());
            GraphIndividual subgraph = species.createNewGraph( null, state, localStartNode, localEndNode, nodesToConsider );

            // Add the new subgraph into the existing candidate
            species.fitMutatedSubgraph(init, graph, subgraph, disconnectedInput, disconnectedOutput);

            // Remove any dangling nodes
            init.removeDanglingNodes( graph );
            init.countGraphElements( graph );
        }

        return n;
    }


    /**
     * Removes nodes to be replaced during mutation and its associated edges. Based on the
     * nodes removed, it determines the inputs and outputs required by that subpart of the
     * graph.
     *
     * @param numNodes - Number of nodes to be removed (greater or equal to 1)
     * @param selected - The root of the mutation removal process
     * @param graph - The original graph to be mutated
     * @param taskInput - Set to collect the inputs required by the removed subpart
     * @param taskOutput - Set to collect the outputs required by the removed subpart
     */
    private void removeMutationNodes(GraphSpecies species, int numNodes, Node selected, GraphIndividual graph, Set<String> taskInput, Set<String> taskOutput, Map<Node, Set<String>> disconnectedInput, Set<Node> disconnectedOutput) {
        if (numNodes < 1)
            throw new RuntimeException(String.format("The number of nodes requested to be removed during mutation was %d; it should always greater than 0.", numNodes));

        Set<Node> mutationNodes = species.selectNodes(selected, numNodes);

        // Now remove all selected mutation nodes and associated edges
        Set<Edge> mutationEdges = new HashSet<Edge>();

        // Remove nodes
        for (Node node : mutationNodes) {
            graph.nodeMap.remove( node.getName() );
            graph.considerableNodeMap.remove( node.getName() );

            for (Edge e : node.getIncomingEdgeList()) {
                mutationEdges.add( e );
                e.getFromNode().getOutgoingEdgeList().remove( e );
            }
            for (Edge e : node.getOutgoingEdgeList()) {
                mutationEdges.add( e );
                e.getToNode().getIncomingEdgeList().remove( e );
            }
        }

        // Remove edges, and figure out what the required inputs and outputs are
        for (Edge edge : mutationEdges) {
            graph.edgeList.remove( edge );
            graph.considerableEdgeList.remove( edge );

            // If the edge is coming from a service that has not been deleted, add its values as available inputs
            if(graph.nodeMap.containsKey( edge.getFromNode().getName())){
                taskInput.addAll(edge.getIntersect());
                disconnectedOutput.add(graph.nodeMap.get(edge.getFromNode().getName()));
            }
            // Else if edge is going to a service that has not been deleted, add its values as required outputs
            else if(graph.nodeMap.containsKey( edge.getToNode().getName())){
                taskOutput.addAll( edge.getIntersect());
                Set<String> discInputs = disconnectedInput.get(graph.nodeMap.get(edge.getToNode().getName()));
                if (discInputs == null) {
                	discInputs = new HashSet<String>(edge.getIntersect());
                	disconnectedInput.put(graph.nodeMap.get(edge.getToNode().getName()), discInputs);
                }
                else {
                	discInputs.addAll(edge.getIntersect());
                }
            }
        }
    }
}
