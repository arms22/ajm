package hydra.ajm;

import processing.core.PApplet;
import ddf.minim.AudioListener;
import ddf.minim.effects.BandPass;
import ddf.minim.effects.LowPassFS;

public class FSKSignalAnalyzer /* extends Thread */implements AudioListener {

	// private static final int INACTIVE = -9;
	private static final int START = 0;
	private static final int DATA = 8;
	private static final int STOP = 9;

	private float bitPeriodInSample;

	private BandPass bpf_hf;
	private BandPass bpf_lf;
	private LowPassFS lpf_br;

	// private int decState = INACTIVE;
	private int decState = START;
	private float decodeCount = 0;
	private float highCount = 0;
	private float lowCount = 0;
	private int decByte;
	private ByteFIFO readFifo;

	// private Vector<Buffer> bufferQueue;

	public float[] asig;
	public float[] bsig;

	// for debug
	private boolean debug = false;
	public float[] bits;
	public float[] state;
	public float[] normal;
	public float[] in;

	public FSKSignalAnalyzer(float highFreq, float lowFreq, float bitRate,
			float sampleRate, int bufferSize) {

		bitPeriodInSample = sampleRate / bitRate;

		bpf_hf = new BandPass(highFreq, (highFreq - lowFreq) / 4, sampleRate);
//		PApplet.println("bpf_hf pass " + bpf_hf.frequency() + " width "
//				+ bpf_hf.getBandWidth());

		bpf_lf = new BandPass(lowFreq, (highFreq - lowFreq) / 4, sampleRate);
//		PApplet.println("bpf_lf pass " + bpf_lf.frequency() + " width "
//				+ bpf_lf.getBandWidth());

		lpf_br = new LowPassFS(bitRate, sampleRate);

		readFifo = new ByteFIFO(9);
		// bufferQueue = new Vector<Buffer>(NUMBER_OF_BUFFER);

		asig = new float[bufferSize];
		bsig = new float[bufferSize];
		bits = new float[bufferSize];
		state = new float[bufferSize];
		normal = new float[bufferSize];
		in = new float[bufferSize];

		reset();
	}

	public synchronized void reset() {
		// decState = INACTIVE;
		decState = START;
		decodeCount = 0;
		highCount = 0;
		lowCount = 0;
		readFifo.clear();
	}

	// class Buffer {
	// byte[] bitStream;
	// float[] samples;
	//
	// public Buffer(float[] signal) {
	// samples = signal.clone();
	// bitStream = new byte[samples.length];
	// }
	//
	// public Buffer(float[] left, float[] right) {
	// samples = new float[left.length];
	// for (int i = 0; i < left.length; i++) {
	// samples[i] = (left[i] + right[i]) / 2;
	// }
	// bitStream = new byte[samples.length];
	// }
	//
	// public float level() {
	// float level = 0;
	// int cnt = 0;
	// for (int i = 0; i < samples.length; i++) {
	// float value = samples[i];
	// if (value > 0.1f) {
	// level += (value * value);
	// cnt++;
	// }
	// }
	// level /= cnt;
	// level = (float) Math.sqrt(level);
	// return level;
	// }
	// }

	public void samples(float[] signal) {
		demodulate(signal);
		// if (bufferQueue.capacity() < bufferQueue.size()) {
		// bufferQueue.add(new Buffer(signal));
		// } else {
		// PApplet.println("FSKSignalAnalyzer: buffer overflow");
		// }
	}

	public void samples(float[] left, float[] right) {
		float[] samples = new float[left.length];
		for (int i = 0; i < samples.length; i++) {
			samples[i] = (left[i] + right[i]) / 2;
		}
		demodulate(samples);
		// if (bufferQueue.capacity() < bufferQueue.size()) {
		// bufferQueue.add(new Buffer(left, right));
		// } else {
		// PApplet.println("FSKSignalAnalyzer: buffer overflow");
		// }
	}

	private void demodulate(float[] signal) {

		System.arraycopy(signal, 0, asig, 0, asig.length);
		System.arraycopy(signal, 0, bsig, 0, asig.length);

		in = signal;

		// find max level
		float max = 0.01f;
		for (int i = 0; i < signal.length; i++) {
			max = Math.max(max, Math.abs(signal[i]));
		}

		// Normalization
		float cmp = 0;
		int sample = 0;
		for (int i = 0; i < signal.length; i++) {
			float v = signal[i];
			v = v * (1.0f / max);
			asig[i] = v;
			bsig[i] = v;
			normal[i] = v;
			cmp += (v * v);
			sample++;
		}
		cmp /= sample;
		cmp = (float) Math.sqrt(cmp);
		cmp = cmp * 0.5f;

		// Band Pass Filter
		bpf_hf.process(asig);
		bpf_lf.process(bsig);

		// Rectification
		for (int i = 0; i < asig.length; i++) {
			asig[i] = Math.abs(asig[i]);
			bsig[i] = Math.abs(bsig[i]);
		}

		// Low Pass Filter
		lpf_br.process(asig);
		lpf_br.process(bsig);

		// Compare
		for (int i = 0; i < asig.length; i++) {
			bits[i] = 0;
			if (asig[i] > cmp) {
				bits[i] += 1;
			}
			if (bsig[i] > cmp) {
				bits[i] -= 1;
			}

			decodeBit(bits[i]);

			state[i] = decState;
		}
	}

	private void decodeBit(float bit) {

		if (bit > 0) {
			highCount++;
		} else if (bit < 0) {
			lowCount++;
		}

		if (decState == START) {
			if (lowCount > highCount) {
				decodeCount++;
				if (decodeCount >= bitPeriodInSample) {
					decodeCount -= bitPeriodInSample;
					decState++;
					decByte = 0;
					lowCount = 0;
				}
			} else if (highCount > 0) {
				decodeCount = lowCount = highCount = 0;
			}
		} else {
			decodeCount++;
			if (decodeCount >= bitPeriodInSample) {
				decodeCount -= bitPeriodInSample;

				if (debug) {
					PApplet.println("FSKSignalAnalyzer: decodeBit " + highCount
							+ "," + lowCount);
				}

				int bitData;
				if (highCount > lowCount) {
					highCount = 0;
					bitData = 128;
				} else {
					lowCount = 0;
					bitData = 0;
				}

				if (decState <= DATA) {
					decByte >>>= 1;
					decByte |= bitData;
					decState++;
					if (debug) {
						PApplet.println("FSKSignalAnalyzer: DATA decByte "
								+ decByte + " bitData " + bitData);
					}
				} else if (decState == STOP) {
					readFifo.add((byte) (decByte & 0xff));
					decState = START;
					if (debug) {
						PApplet.println("FSKSignalAnalyzer: START");
					}
				}
			}
		}
	}

	public int available() {
		return readFifo.size();
	}

	public int read() {
		if (readFifo.empty() == true)
			return -1;
		return readFifo.remove();
	}

	public synchronized void setFreq(float h_freq, float h_bw, float l_freq,
			float l_bw) {
		bpf_hf.setFreq(h_freq);
		bpf_hf.setBandWidth(h_bw);
		bpf_lf.setFreq(l_freq);
		bpf_lf.setBandWidth(l_bw);
	}
}
