package hydra.ajm;

import ddf.minim.AudioSignal;
import processing.core.PApplet;

public class FSKSignalWave implements AudioSignal {

	private static final float TWO_PI = (float) (2 * Math.PI);
	private static final int SIN_TABLE_LENGTH = 441;
	private static final byte PREAMBLE = (byte) 0xff;
	private static final int NUMBER_OF_PREAMBLE = 4;

	private float sampleRate;
	private float bitPeriodInSample;
	private float sinStep;
	private float highFreqStepSize;
	private float lowFreqStepSize;
	private float[] sinTable;

	private float[] audioBuffer;
	private int audioBufferSize;
	private int audioBufferIndex;
	private boolean preambleRequired;

	private ByteFIFO writeFifo;

	private boolean debug = false;

	public FSKSignalWave(float highFreq, float lowFreq, float bitRate,
			float sampleRate) {

		this.sampleRate = sampleRate;
		bitPeriodInSample = sampleRate / bitRate;
		highFreqStepSize = highFreq * SIN_TABLE_LENGTH / sampleRate;
		lowFreqStepSize = lowFreq * SIN_TABLE_LENGTH / sampleRate;

		writeFifo = new ByteFIFO(9);

		if (debug) {
			PApplet.println(writeFifo.size());
			PApplet.println(writeFifo.free());
		}

		audioBuffer = new float[(int) (sampleRate / bitRate
				* NUMBER_OF_PREAMBLE * 8)];

		if (debug) {
			PApplet.println("FSKSignalWave: highFreqStepSize "
					+ highFreqStepSize);
			PApplet.println("FSKSignalWave: lowFreqStepSize " + lowFreqStepSize);
			PApplet.println("FSKSignalWave: bitPeriodInSample "
					+ bitPeriodInSample);
		}

		sinTable = new float[SIN_TABLE_LENGTH];
		for (int i = 0; i < SIN_TABLE_LENGTH; i++) {
			sinTable[i] = (float) Math.sin(i * TWO_PI / SIN_TABLE_LENGTH);
		}

		reset();
	}

	public void reset() {
		sinStep = 0;
		audioBufferIndex = audioBufferSize = 0;
		preambleRequired = true;
		writeFifo.clear();
	}

	private void addWaveform(float stepSize) {
		for (int i = 0; i < (int) (bitPeriodInSample); i++) {
			audioBuffer[audioBufferSize++] = sinTable[(int) sinStep];
			sinStep += stepSize;
			if (sinStep >= SIN_TABLE_LENGTH) {
				sinStep -= SIN_TABLE_LENGTH;
			}
		}
	}

	private void addPostamble() {
		addWaveform(highFreqStepSize);
		addWaveform(highFreqStepSize);
		addWaveform(highFreqStepSize);
		addWaveform(highFreqStepSize);
		addWaveform(highFreqStepSize);
		addWaveform(highFreqStepSize);
		addWaveform(highFreqStepSize);
		do {
			audioBuffer[audioBufferSize++] = sinTable[(int) sinStep];
			sinStep += highFreqStepSize;
			if (sinStep >= SIN_TABLE_LENGTH) {
				sinStep -= SIN_TABLE_LENGTH;
				break;
			}
		} while (true);
	}

	private void addByte(byte value) {
		addWaveform(lowFreqStepSize);
		addRawByte(value);
		addWaveform(highFreqStepSize);
	}

	private void addRawByte(byte value) {
		for (int i = 0; i < 8; i++) {
			if ((value & 1) == 1) {
				addWaveform(highFreqStepSize);
			} else {
				addWaveform(lowFreqStepSize);
			}
			value >>= 1;
		}
	}

	private final float generate() {
		float value = 0;
		if (audioBufferIndex < audioBufferSize) {
			value = audioBuffer[audioBufferIndex++];
		} else {
			if (writeFifo.size() > 0) {
				if (debug) {
					PApplet.println("FSKSignalWave: writeFifo.size()"
							+ writeFifo.size());
				}
				audioBufferIndex = audioBufferSize = 0;
				if (preambleRequired) {
					for (int i = 0; i < NUMBER_OF_PREAMBLE; i++) {
						addRawByte(PREAMBLE);
					}
					preambleRequired = false;
					if (debug) {
						PApplet.println("FSKSignalWave: preambleRequired");
					}
				} else {
					addByte(writeFifo.remove());
					if (debug) {
						PApplet.println("FSKSignalWave: addByte()");
					}
				}
				value = audioBuffer[audioBufferIndex++];
			} else {
				if (preambleRequired) {
					;
				} else {
					preambleRequired = true;
					audioBufferIndex = audioBufferSize = 0;
					addPostamble();
					value = audioBuffer[audioBufferIndex++];

					if (debug) {
						PApplet.println("FSKSignalWave: addPostamble();");
					}
				}
			}
		}
		return value;
	}

	public void generate(float[] signal) {
		for (int i = 0; i < signal.length; i++) {
			signal[i] = generate();
		}
	}

	public void generate(float[] left, float[] right) {
		for (int i = 0; i < left.length; i++) {
			left[i] = generate();
			right[i] = left[i];
		}
	}

	public void write(byte value) {
		writeFifo.add(value);
	}

	public synchronized void setFreq(float h_freq, float l_freq) {
		highFreqStepSize = h_freq * SIN_TABLE_LENGTH / sampleRate;
		lowFreqStepSize = l_freq * SIN_TABLE_LENGTH / sampleRate;
	}
}
