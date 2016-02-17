

/*
Copyright 2006 by Sean Luke
Licensed under the Academic Free License version 3.0
See the file "LICENSE" for more information
 */
package ec.graph;
import ec.*;
import ec.simple.SimpleEvolutionState;
import ec.util.Checkpoint;


/**
 * A GraphState is an extension of SimpleEvolutionState. It is able to keep track of the number of node and edge
 * optimizations.
 * @author yanlong
 *
 */
public class GraphState extends SimpleEvolutionState
{
	private int totalNodeOpt = 0;//count the total number of nodeOpt
	private int totalEdgeOpt = 0;//count the total number of edgeOpt

	/**
	 * setter for totalNodeOpt
	 * @param totalNodeOpt
	 */
	public void setTotalNodeOpt(int totalNodeOpt) {
		this.totalNodeOpt = totalNodeOpt;
	}

	/**
	 * setter for totalEdgeOpt
	 * @param totalEdgeOpt
	 */
	public void setTotalEdgeOpt(int totalEdgeOpt) {
		this.totalEdgeOpt = totalEdgeOpt;
	}

	/**
	 * getter for totalNodeOpt
	 * @return
	 */
	public int getTotalNodeOpt() {
		return totalNodeOpt;
	}

	/**
	 * getter for totalEdgeOpt
	 * @return
	 */
	public int getTotalEdgeOpt() {
		return totalEdgeOpt;
	}


}