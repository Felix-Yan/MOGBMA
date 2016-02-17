package ec.graph;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ec.EvolutionState;
import ec.Individual;
import ec.Problem;
import ec.multiobjective.MultiObjectiveFitness;
import ec.simple.SimpleFitness;
import ec.simple.SimpleProblemForm;
import ec.util.Log;

public class GraphEvol extends Problem implements SimpleProblemForm {

	@Override
	public void evaluate(EvolutionState state, Individual ind, 
			int subpopulation, int threadnum) {
		GraphInitializer init = (GraphInitializer) state.initializer;
		if (init.runningOwls) {
			evaluateOwls(init, state, ind, subpopulation, threadnum);
		}
		else {
			evaluateQoS(init, state, ind, subpopulation, threadnum);
		}
	}

	public void evaluateQoS(GraphInitializer init, EvolutionState state, 
			Individual ind, int subpopulation, int threadnum) {
		if (ind.evaluated) return;   //don't evaluate the individual if it's already evaluated
		if (!(ind instanceof GraphIndividual))
			state.output.fatal("Whoa!  It's not a GraphIndividual!!!",null);
		GraphIndividual ind2 = (GraphIndividual)ind;

		double a = 1.0;
		double r = 1.0;
		double t = 0.0;
		double c = 0.0;
		double[] objectives = ((MultiObjectiveFitness)ind.fitness).getObjectives();

		for (Node n : ind2.considerableNodeMap.values()) {
			double[] qos = n.getQos();
			
			//debug
			/*System.out.println("A: "+qos[GraphInitializer.AVAILABILITY]);
			System.out.println("R: "+qos[GraphInitializer.RELIABILITY]);*/
			
			a *= qos[GraphInitializer.AVAILABILITY];
			r *= qos[GraphInitializer.RELIABILITY];
			c += qos[GraphInitializer.COST];
		}

		// Calculate longest time
		t = findLongestPath(ind2);
		
		objectives[0] = t;
		objectives[1] = c;
		objectives[2] = a;
		objectives[3] = r;

		ind2.setAvailability(a);
		ind2.setReliability(r);
		ind2.setCost(c);
		ind2.setTime(t);
		
		((MultiObjectiveFitness)ind.fitness).setObjectives(state, objectives);

		ind2.evaluated = true;
	}

	public void evaluateOwls(GraphInitializer init, EvolutionState state, Individual ind, int subpopulation, int threadnum) {

		if (ind.evaluated) return;   //don't evaluate the individual if it's already evaluated
		if (!(ind instanceof GraphIndividual))
			state.output.fatal("Whoa!  It's not a GraphIndividual!!!",null);
		GraphIndividual ind2 = (GraphIndividual)ind;

		// Calculate longest time
		int runPath = findLongestPath2(ind2) - 1;
		ind2.longestPathLength = runPath;
		ind2.numAtomicServices = (ind2.considerableNodeMap.size() - 2);
		boolean isIdeal = runPath == init.idealPathLength && ind2.numAtomicServices == init.idealNumAtomic;

		double fitness = 0.5 * (1.0 / runPath) + 0.5 * (1.0/ ind2.numAtomicServices);
		//double fitness = (100 - runPath) + (100 - ind2.numAtomicServices);

		((SimpleFitness)ind2.fitness).setFitness(state,
				// ...the fitness...
				fitness,
				///... is the individual ideal?  Indicate here...
				isIdeal);

		ind2.evaluated = true;
	}

	/**
	 * Uses the Bellman-Ford algorithm with negative weights to find the longest
	 * path in an acyclic directed graph. Length is calculated by the sum of execution time.
	 *
	 * @param g
	 * @return list of edges composing longest path
	 */
	private double findLongestPath(GraphIndividual g) {
		Map<String, Double> distance = new HashMap<String, Double>();
		Map<String, Node> predecessor = new HashMap<String, Node>();

		// Step 1: initialize graph
		for (Node node : g.considerableNodeMap.values()) {
			if (node.getName().equals("start"))
				distance.put(node.getName(), 0.0);
			else
				distance.put(node.getName(), Double.POSITIVE_INFINITY);
		}

		// Step 2: relax edges repeatedly
		for (int i = 1; i < g.considerableNodeMap.size(); i++) {
			for (Edge e : g.considerableEdgeList) {
				//debug
				if(!distance.containsKey(e.getFromNode().getName())){
					System.out.println("ToNode=="+e.getToNode().getName());
					System.out.println("FromNode=="+e.getFromNode().getName());
				}

				if ((distance.get(
						e.getFromNode()
						.getName()) -
						e.getToNode().getQos()[GraphInitializer.TIME])
						< distance.get(e.getToNode().getName())) {
					distance.put(e.getToNode().getName(), (distance.get(e.getFromNode().getName()) - e.getToNode().getQos()[GraphInitializer.TIME]));
					predecessor.put(e.getToNode().getName(), e.getFromNode());
				}
			}
		}

		// Now retrieve total cost
		Node pre = predecessor.get("end");
		double totalTime = 0.0;

		while (pre != null) {
			totalTime += pre.getQos()[GraphInitializer.TIME];
			pre = predecessor.get(pre.getName());
		}

		return totalTime;
	}

	/**
	 * Uses the Bellman-Ford algorithm with negative weights to find the longest
	 * path in an acyclic directed graph. Length is calculated by the number of nodes.
	 *
	 * @param g
	 * @return list of edges composing longest path
	 */
	private int findLongestPath2(GraphIndividual g) {
		Map<String, Integer> distance = new HashMap<String, Integer>();
		Map<String, Node> predecessor = new HashMap<String, Node>();

		// Step 1: initialize graph
		for (Node node : g.considerableNodeMap.values()) {
			if (node.getName().equals("start"))
				distance.put(node.getName(), 0);
			else
				distance.put(node.getName(), Integer.MAX_VALUE);
		}

		// Step 2: relax edges repeatedly
		for (int i = 1; i < g.considerableNodeMap.size(); i++) {
			for (Edge e : g.considerableEdgeList) {
				if ((distance.get(e.getFromNode().getName()) - 1)
						< distance.get(e.getToNode().getName())) {
					distance.put(e.getToNode().getName(), (distance.get(e.getFromNode().getName()) - 1));
					predecessor.put(e.getToNode().getName(), e.getFromNode());
				}
			}
		}

		// Now retrieve total cost
		Node pre = predecessor.get("end");
		int totalTime = 0;
		while (pre != null) {
			totalTime += 1;
			pre = predecessor.get(pre.getName());
		}

		return totalTime;
	}

	//	@Override
	//	public void describe(EvolutionState state, Individual ind, int subpopulation, int thread, int log) {
	//		Log l = state.output.getLog(log);
	//		GraphIndividual graph = (GraphIndividual) ind;
	//
	//		System.out.println(String.format("runPath= %d #atomicProcess= %d\n", graph.longestPathLength, graph.considerableNodeMap.size() - 2));
	//		l.writer.append(String.format("runPath= %d #atomicProcess= %d\n", graph.longestPathLength, graph.considerableNodeMap.size() - 2));
	//	}
}
