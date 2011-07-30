/**
 * you can put a one sentence description of your library here.
 *
 * ##copyright##
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General
 * Public License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307  USA
 *
 * @author		##author##
 * @modified	##date##
 * @version		##version##
 */

package hydra.ajm;

import processing.core.*;
import ddf.minim.Minim;

/**
 * This is a template class and can be used to start a new processing library or
 * tool. Make sure you rename this class as well as the name of the example
 * package 'template' to your own lobrary or tool naming convention.
 *
 * @example Hello
 *
 *          (the tag @example followed by the name of an example included in
 *          folder 'examples' will automatically include the example in the
 *          javadoc.)
 *
 */

public class Ajm {
	PApplet app;
	Minim minim;

	/**
	 * a Constructor, usually called in the setup() method in your sketch to
	 * initialize and start the library.
	 *
	 * @example Hello
	 * @param theParent
	 */
	public Ajm(PApplet theParent) {
		app = theParent;
		minim = new Minim(app);
	}

	public FSKModem createFSKModem() {
		return new FSKModem(minim, 7350, 4900, 1225, 7350, 4900, 1225);
	}

	public FSKModem createFSKModem(float highFreq, float lowFreq, float bitRate) {
		return new FSKModem(minim, highFreq, lowFreq, bitRate, highFreq,
				lowFreq, bitRate);
	}

	public FSKModem createFSKModem(float highFreq, float lowFreq,
			float bitRate, float highFreq2, float lowFreq2, float bitRate2) {
		return new FSKModem(minim, highFreq, lowFreq, bitRate, highFreq2,
				lowFreq2, bitRate2);
	}

	/**
	 * Stops AudioJackModem.
	 *
	 * A call to this method should be placed inside of the stop() function of
	 * your sketch. We expect that implemenations of the AudioJackModem
	 * interface made need to do some cleanup, so this is how we tell them it's
	 * time.
	 *
	 */
	public void stop() {
		minim.stop();
	}
}
