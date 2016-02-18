

/*
Copyright 2006 by Sean Luke
Licensed under the Academic Free License version 3.0
See the file "LICENSE" for more information
 */
package ec.graph;
import java.util.HashMap;

import ec.*;
import ec.simple.SimpleEvolutionState;
import ec.util.Checkpoint;
import ec.util.Parameter;


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

	/**
	 * This overrides the setup method in EvoluationState
	 * Population will be generated here
	 */
	public void setup(final EvolutionState state, final Parameter base)
    {
    Parameter p;
    
    // set up the per-thread data
    data = new HashMap[random.length];
    for(int i = 0; i < data.length; i++)
        data[i] = new HashMap();

    // we ignore the base, it's worthless anyway for EvolutionState

    p = new Parameter(P_CHECKPOINT);
    checkpoint = parameters.getBoolean(p,null,false);

    p = new Parameter(P_CHECKPOINTPREFIX);
    checkpointPrefix = parameters.getString(p,null);
    if (checkpointPrefix==null)
        {
        // check for the old-style checkpoint prefix parameter
        Parameter p2 = new Parameter("prefix");
        checkpointPrefix = parameters.getString(p2,null);
        if (checkpointPrefix==null)
            {
            output.fatal("No checkpoint prefix specified.",p);  // indicate the new style, not old parameter
            }
        else
            {
            output.warning("The parameter \"prefix\" is deprecated.  Please use \"checkpoint-prefix\".", p2);
            }
        }
    else
        {
        // check for the old-style checkpoint prefix parameter as an acciental duplicate
        Parameter p2 = new Parameter("prefix");
        if (parameters.getString(p2,null) != null)
            {
            output.warning("You have BOTH the deprecated parameter \"prefix\" and its replacement \"checkpoint-prefix\" defined.  The replacement will be used,  Please remove the \"prefix\" parameter.", p2);
            }
        
        }
        

    p = new Parameter(P_CHECKPOINTMODULO);
    checkpointModulo = parameters.getInt(p,null,1);
    if (checkpointModulo==0)
        output.fatal("The checkpoint modulo must be an integer >0.",p);
    
    p = new Parameter(P_CHECKPOINTDIRECTORY);
    if (parameters.exists(p, null))
        {
        checkpointDirectory = parameters.getFile(p,null);
        if (checkpointDirectory==null)
            output.fatal("The checkpoint directory name is invalid: " + checkpointDirectory, p);
        if (!checkpointDirectory.isDirectory())
            output.fatal("The checkpoint directory location is not a directory: " + checkpointDirectory, p);
        }
    else checkpointDirectory = null;
        
    
    // load evaluations, or generations, or both
        
    p = new Parameter(P_EVALUATIONS);
    if (parameters.exists(p, null))
        {
        numEvaluations = parameters.getInt(p, null, 1);  // 0 would be UNDEFINED
        if (numEvaluations <= 0)
            output.fatal("If defined, the number of evaluations must be an integer >= 1", p, null);
        }
            
    p = new Parameter(P_GENERATIONS);
    if (parameters.exists(p, null))
        {
        numGenerations = parameters.getInt(p, null, 1);  // 0 would be UDEFINED                 
                            
        if (numGenerations <= 0)
            output.fatal("If defined, the number of generations must be an integer >= 1.", p, null);

        if (numEvaluations != UNDEFINED)  // both defined
            {
            state.output.warning("Both generations and evaluations defined: generations will be ignored and computed from the evaluations.");
            numGenerations = UNDEFINED;
            }
        }
    else if (numEvaluations == UNDEFINED)  // uh oh, something must be defined
        output.fatal("Either evaluations or generations must be defined.", new Parameter(P_GENERATIONS), new Parameter(P_EVALUATIONS));

    
    p=new Parameter(P_QUITONRUNCOMPLETE);
    quitOnRunComplete = parameters.getBoolean(p,null,false);


    /* Set up the singletons */
    p=new Parameter(P_INITIALIZER);
    initializer = (Initializer)
        (parameters.getInstanceForParameter(p,null,Initializer.class));
    initializer.setup(this,p);
    //Generate population here for breeder use soon
    population = initializer.initialPopulation(this, 0); // unthreaded

    p=new Parameter(P_FINISHER);
    finisher = (Finisher)
        (parameters.getInstanceForParameter(p,null,Finisher.class));
    finisher.setup(this,p);

    p=new Parameter(P_BREEDER);
    breeder = (Breeder)
        (parameters.getInstanceForParameter(p,null,Breeder.class));
    breeder.setup(this,p);

    p=new Parameter(P_EVALUATOR);
    evaluator = (Evaluator)
        (parameters.getInstanceForParameter(p,null,Evaluator.class));
    evaluator.setup(this,p);

    p=new Parameter(P_STATISTICS);
    statistics = (Statistics)
        (parameters.getInstanceForParameterEq(p,null,Statistics.class));
    statistics.setup(this,p);
    
    p=new Parameter(P_EXCHANGER);
    exchanger = (Exchanger)
        (parameters.getInstanceForParameter(p,null,Exchanger.class));
    exchanger.setup(this,p);
            
    generation = 0;
    }
	
	/**
	 * This overrides the same name method in SimpleEvolutionState
	 * Population will not be generated here
	 */
	public void startFresh() 
    {
    output.message("Setting up");
    setup(this,null);  // a garbage Parameter

    // POPULATION INITIALIZATION
    output.message("Initializing Generation 0");
    statistics.preInitializationStatistics(this);
    //population = initializer.initialPopulation(this, 0); // unthreaded
    statistics.postInitializationStatistics(this);
    
    // Compute generations from evaluations if necessary
    if (numEvaluations > UNDEFINED)
        {
        // compute a generation's number of individuals
        int generationSize = 0;
        for (int sub=0; sub < population.subpops.length; sub++)  
            { 
            generationSize += population.subpops[sub].individuals.length;  // so our sum total 'generationSize' will be the initial total number of individuals
            }
            
        if (numEvaluations < generationSize)
            {
            numEvaluations = generationSize;
            numGenerations = 1;
            output.warning("Using evaluations, but evaluations is less than the initial total population size (" + generationSize + ").  Setting to the populatiion size.");
            }
        else 
            {
            if (numEvaluations % generationSize != 0)
                output.warning("Using evaluations, but initial total population size does not divide evenly into it.  Modifying evaluations to a smaller value ("
                    + ((numEvaluations / generationSize) * generationSize) +") which divides evenly.");  // note integer division
            numGenerations = (int)(numEvaluations / generationSize);  // note integer division
            numEvaluations = numGenerations * generationSize;
            } 
        output.message("Generations will be " + numGenerations);
        }    

    // INITIALIZE CONTACTS -- done after initialization to allow
    // a hook for the user to do things in Initializer before
    // an attempt is made to connect to island models etc.
    exchanger.initializeContacts(this);
    evaluator.initializeContacts(this);
    }

}