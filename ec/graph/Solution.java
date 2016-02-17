/*package ec.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.yimei.multiobjective.core.Arc;
import com.yimei.multiobjective.core.Vertex;
import com.yimei.multiobjective.problem.Problem;

import yimei.util.Utilities;

public class Solution implements Comparable<Solution> {

	// structure
	private List<Vertex> tour; // the tour is a sequence of vertex
	private List<Double> time; // the arrival & departure time of each vertex along the tour
	private List<Double> latestTime; // the latest arrival time of each vertex along the tour without violating the time budget

	// objective

	// the total scores of the collected locations (only one total score for single objective)
	// the scores are negated to transform maximization to minimization
	private List<Double> scores;

	private List<Double> normScores;

	public double fitness;

	// for nondominated sorting
	public int rank;
	public double crowdingDistance;


	public Solution(int numScores) {
		tour = new ArrayList<Vertex>();
		time = new ArrayList<Double>();
		latestTime = new ArrayList<Double>();
		scores = new ArrayList<Double>(Collections.nCopies(numScores, 0.0));
	}

	public Solution(Solution sol) {
		this.tour = new ArrayList<Vertex>(sol.tour);
		this.time = new ArrayList<Double>(sol.time);
		this.latestTime = new ArrayList<Double>(sol.latestTime);
		this.scores = new ArrayList<Double>(sol.scores);
	}

	public List<Vertex> tour() {
		return tour;
	}

	public Vertex vertex(int index) {
		return tour.get(index);
	}

	public double timeOf(int index) {
		return time.get(index);
	}

	public double latestTimeOf(int index) {
		return latestTime.get(index);
	}

	public List<Double> scores() {
		return scores;
	}

	public double score(int index) {
		return scores.get(index);
	}

	public List<Double> normScores() {
		return normScores;
	}

	public double normScore(int index) {
		return normScores.get(index);
	}

	public void evaluate() {
		for (int i = 0; i < scores.size(); i++) {
			double score = 0.0;
			for (int j = 0; j < tour.size(); j++) {
				score += tour.get(j).score(i);
			}
			scores.set(i, score);
		}
	}

	public void setTour(List<Vertex> tour) {
		this.tour = tour;
	}

	public void setScore(int index, double score) {
		scores.set(index, score);
	}

	public void setNormScore(int index, double normScore) {
		normScores.set(index, normScore);
	}

	public boolean feasibleToAdd(Vertex v, int index, Problem problem) {

		double t1 = problem.travelTime(tour.get(index-1), v, time.get(index-1));

		if (time.get(index-1) + t1 > problem.startTime + problem.timeLimit()) {
			return false;
		}

		double t2 = problem.travelTime(v, tour.get(index), time.get(index-1) + t1);

//		if (v.id() == 1714) {
//			System.out.println("");
//			System.out.println("checking feasibility");
//			v.printMe();
//			System.out.println("index = " + index);
//			System.out.println("t1 = " + t1 + ", t2 = " + t2);
//		}


		return (time.get(index-1) + t1 + t2 < latestTime.get(index));
	}

	public void add(Vertex v, int index, Problem problem) {
//		this.printMe();
//		System.out.println("index = " + index);
//		v.printMe();

		tour.add(index, v);
		time.add(index, 0.0);
		latestTime.add(index, 0.0);
		updateTime(index, problem);
		updateLatestTime(index, problem);
		addVertexScores(v);
	}

	public boolean feasibleToRemove(int index, Problem problem) {

		if (index == 0 || index == tour.size()-1)
			return false;

		double t1 = problem.travelTime(tour.get(index-1), tour.get(index+1), time.get(index-1));

//		System.out.println("");
//		System.out.println("checking feasibility");
//		v.printMe();
//		System.out.println("index = " + index);
//		System.out.println("t1 = " + t1 + ", t2 = " + t2);

		return (time.get(index-1) + t1 < latestTime.get(index+1));
	}

	public void remove(int index, Problem problem) {
//		this.printMe();
//		System.out.println("index = " + index);
		Vertex v = tour.get(index);
		reduceVertexScores(v);
		tour.remove(index);
		time.remove(index);
		latestTime.remove(index);

		if (index < tour.size()) {
			updateTime(index, problem);
		}
		if (index > 0) {
			updateLatestTime(index-1, problem);
		}
//		this.printMe();
	}

	// update the time from index onward
	public void updateTime(int index, Problem problem) {
		if (index == 0) {
			time.set(index, problem.startTime);
		}
		else {
			double t = problem.travelTime(tour.get(index-1), tour.get(index), time.get(index-1));
			time.set(index, time.get(index-1) + t);
		}

		for (int i = index+1; i < tour.size(); i++) {
			double t = problem.travelTime(tour.get(i-1), tour.get(i), time.get(i-1));
			time.set(i, time.get(i-1) + t);
		}
	}

	// update the latest time from index backward
	public void updateLatestTime(int index, Problem problem) {
		if (index == tour.size()-1) {
			latestTime.set(index, problem.startTime + problem.timeLimit());
		}
		else {
//			tour.get(index).printMe();
//			tour.get(index+1).printMe();
//			System.out.println("arrival time = " + latestTime.get(index+1));
			double t = problem.travelTimeReverse(tour.get(index), tour.get(index+1), latestTime.get(index+1));
//			System.out.println("t = " + t);
			latestTime.set(index, latestTime.get(index+1) - t);
		}

		for (int i = index-1; i > -1; i--) {
//			if (tour.get(index).id() == 1714) {
//				tour.get(i).printMe();
//				tour.get(i+1).printMe();
//				System.out.println("arrival time = " + latestTime.get(i+1));
//			}
			double t = problem.travelTimeReverse(tour.get(i), tour.get(i+1), latestTime.get(i+1));

			latestTime.set(i, latestTime.get(i+1) - t);
		}
	}


	public void addVertexScores(Vertex v) {
		for (int i = 0; i < scores.size(); i++) {
			scores.set(i, scores.get(i) + v.score(i));
		}
	}

	public void reduceVertexScores(Vertex v) {
		for (int i = 0; i < scores.size(); i++) {
			scores.set(i, scores.get(i) - v.score(i));
		}
	}

	public int size() {
		return tour.size();
	}

	public boolean contains(Vertex v) {
		return tour.contains(v);
	}

	public boolean containsArc(Arc arc) {
		for (int i = 0; i < tour.size()-1; i++) {
			Vertex v1 = tour.get(i);
			Vertex v2 = tour.get(i+1);

			if (arc.fromVertex().equals(v1) && arc.toVertex().equals(v2)) {
				return true;
			}
		}

		return false;
	}

	// comparison
	// -1: better; 1: worse; 0: incomparable
	public int dominanceRelation(Solution cmpSol) {
		int numDominated = 0;
		int numDominate = 0;

		for (int i = 0; i < scores.size(); i++) {
			if (this.score(i) > cmpSol.score(i)) {
				numDominate ++;
			}
			else if (this.score(i) < cmpSol.score(i)) {
				numDominated ++;
			}
		}

		if (numDominated == 0) {
			if (numDominate > 0) {
				return -1;
			}
			else {
				return 0;
			}
		}
		else {
			if (numDominate > 0) {
				return 0;
			}
			else {
				return 1;
			}
		}
	}

	public boolean equalScores(Solution cmpSol) {
		for (int i = 0; i < scores.size(); i++) {
			if (this.score(i) != cmpSol.score(i)) {
				return false;
			}
		}

		return true;
	}

	public int compareTo(Solution cmpSol) {
		if (this.rank < cmpSol.rank) {
			return -1;
		}
		else if (this.rank == cmpSol.rank) {
			return 0;
		}
		else {
			return 1;
		}
	}

	public double euclideanDistanceFrom(Solution sol) {

		double distance = Utilities.getEuclideanDistance(scores, sol.scores());

		return distance;
	}

	public String tourString() {
		String ts = "";
		for (Vertex v : tour) {
			ts = ts + v.id() + " ";
		}

		return ts;
	}

	public String scoreString() {
		String ss = "";
		for (double score : scores) {
			ss = ss + score + " ";
		}

		return ss;
	}

	public void printMe() {
		System.out.println("");
		System.out.print("tour: ");
		for (Vertex v : tour) {
			System.out.print(v.id() + " ");
		}
		System.out.println("");
		System.out.println("time: " + time);
		System.out.println("lastest time: " + latestTime);
		System.out.println("scores: " + scores);
	}
}
*/