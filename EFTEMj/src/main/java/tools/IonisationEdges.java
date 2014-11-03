/**
 * EFTEMj - Processing of Energy Filtering TEM images with ImageJ
 *
 * Copyright (c) 2014, Michael Entrup b. Epping <michael.entrup@wwu.de>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package tools;

import java.util.LinkedHashMap;

/**
 * @author Michael Entrup b. Epping <michael.entrup@wwu.de>
 *
 */
public class IonisationEdges {

    /**
     * There is only one instance of {@link IonisationEdges}. That is why the Singleton pattern is used.
     */
    private static final IonisationEdges INSTANCE = new IonisationEdges();

    /**
     * Instead of using the constructor you can get an instance of {@link PluginAPI} by using this method.
     *
     * @return The only instance of {@link PluginAPI}
     */
    public static IonisationEdges getInstance() {
	return INSTANCE;
    }

    private final LinkedHashMap<Integer, String> edges;

    /**
     * A private constructor that creates the {@link LinkedHashMap} edges.
     */
    private IonisationEdges() {
	edges = new LinkedHashMap<Integer, String>();
	edges.put(51, "Magnesium L<sub>2,3</sub>-edge");
	edges.put(73, "Aluminium L<sub>2,3</sub>-edge");
	edges.put(86, "Lead O<sub>2,3</sub>-edge");
	edges.put(96, "Uranium O<sub>4,5</sub>-edge");
	edges.put(99, "Silicon L<sub>2,3</sub>-edge");
	edges.put(132, "Phosphorus L<sub>2,3</sub>-edge");
	edges.put(165, "Sulfur L<sub>2,3</sub>-edge");
	edges.put(188, "Boron K-edge");
	edges.put(227, "Molybdenium M<sub>4,5</sub>-edge");
	edges.put(284, "Carbon K-edge");
	edges.put(346, "Calcium L<sub>3</sub>-edge");
	edges.put(350, "Calcium L<sub>2</sub>-edge");
	edges.put(367, "Silver M<sub>4,5</sub>-edge");
	edges.put(381, "Uranium N<sub>7</sub>-edge");
	edges.put(391, "Uranium N<sub>6</sub>-edge");
	edges.put(401, "Nitrogen K-edge");
	edges.put(456, "Titan L<sub>3</sub>-edge");
	edges.put(462, "Titan L<sub>2</sub>-edge");
	edges.put(512, "Vanadium L<sub>3</sub>-edge");
	edges.put(521, "Vanadium L<sub>2</sub>-edge");
	edges.put(532, "Oxygen K-edge");
	edges.put(575, "Cromium L<sub>3</sub>-edge");
	edges.put(584, "Cromium L<sub>2</sub>-edge");
	edges.put(640, "Manganese L<sub>3</sub>-edge");
	edges.put(651, "Manganese L<sub>2</sub>-edge");
	edges.put(685, "Fluorine K-edge");
	edges.put(708, "Iron L<sub>3</sub>-edge");
	edges.put(721, "Iron L<sub>2</sub>-edge");
	edges.put(779, "Cobalt L<sub>3</sub>-edge");
	edges.put(794, "Cobalt L<sub>2</sub>-edge");
	edges.put(855, "Nickel L<sub>3</sub>-edge");
	edges.put(872, "Nickel L<sub>2</sub>-edge");
	edges.put(931, "Copper L<sub>3</sub>-edge");
	edges.put(951, "Copper L<sub>2</sub>-edge");
	edges.put(1020, "Zinc L<sub>3</sub>-edge");
	edges.put(1043, "Zinc L<sub>2</sub>-edge");
	edges.put(1072, "Natrium K-egde");
	edges.put(1115, "Gallium L<sub>3</sub>-edge");
	edges.put(1142, "Gallium L<sub>2</sub>-edge");
	edges.put(1217, "Germanium L<sub>3</sub>-edge");
	edges.put(1248, "Germanium L<sub>2</sub>-edge");
	edges.put(1305, "Magnesium K-edge");
	edges.put(1560, "Aluminium K-edge");
	edges.put(1839, "Silicon K-edge");
	edges.put(2146, "Phosphorus K-edge");
	edges.put(2206, "Gold M<sub>5</sub>-edge");
	edges.put(2291, "Gold M<sub>4</sub>-edge");
	edges.put(2484, "Lead M<sub>5</sub>-edge");
	edges.put(2586, "Lead M<sub>4</sub>-edge");
	edges.put(2520, "Molybdenium L<sub>3</sub>-edge");
	edges.put(2625, "Molybdenium L<sub>2</sub>-edge");
	edges.put(3351, "Silver L<sub>3</sub>-edge");
	edges.put(3524, "Silver L<sub>2</sub>-edge");
	edges.put(3552, "Uranium M<sub>5</sub>-edge");
	edges.put(3728, "Uranium M<sub>4</sub>-edge");
    }

    public LinkedHashMap<Integer, String> getEdges() {
	return edges;
    }

}
