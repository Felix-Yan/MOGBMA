package ec.graph;

import java.io.DataInputStream;
import java.util.Random;

import ec.util.MersenneTwisterFast;

public class GraphRandom extends Random {
	private MersenneTwisterFast m;

	public GraphRandom(MersenneTwisterFast m) {
		this.m = m;
	}

	@Override
	public boolean nextBoolean() {
		return m.nextBoolean();
	}

	@Override
	public double nextDouble() {
		return m.nextDouble();
	}

	@Override
	public float nextFloat() {
		return m.nextFloat();
	}

	@Override
	public int nextInt() {
		return m.nextInt();
	}

	@Override
	public int nextInt(int n) {
		return m.nextInt(n);
	}

	@Override
	public long nextLong() {
		return m.nextLong();
	}
}
