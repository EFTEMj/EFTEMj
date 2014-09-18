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

import java.util.Arrays;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Macro;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.io.FileInfo;
import ij.measure.Calibration;
import ij.plugin.PlugIn;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

/**
 * @author Michael Entrup b. Epping <michael.entrup@wwu.de>
 * 
 */
public class ExtendedStackToImage implements PlugIn {

    private static final int rgb = 33;
    private static final int COPY_CENTER = 0;
    private static final int COPY_TOP_LEFT = 1;
    private static final int SCALE_SMALL = 2;
    private static final int SCALE_LARGE = 3;
    // begin - new code of ExtendedImageToStack
    /**
     * New in {@link ExtendedImageToStack}: The maximum number of images that allow an user defined order.
     */
    private static final int MAX_IMAGES = 10;
    // end - new code of ExtendedImageToStack
    private static final String[] methods = { "Copy (center)", "Copy (top-left)", "Scale (smallest)", "Scale (largest)" };
    private static int method = COPY_CENTER;
    private static boolean bicubic;
    private static boolean keep;
    private static boolean titlesAsLabels = true;
    private boolean sortByUser = true;
    private String filter;
    private int width, height;
    private int maxWidth, maxHeight;
    private int minWidth, minHeight;
    private int minSize, maxSize;
    private Calibration cal2;
    private int stackType;
    private ImagePlus[] image;
    private String name = "Stack";

    public void run(String arg) {
	convertImagesToStack();
    }

    public void convertImagesToStack() {
	int[] wList = WindowManager.getIDList();
	if (wList == null) {
	    IJ.error("No images are open.");
	    return;
	}
	int count = 0;
	image = new ImagePlus[wList.length];
	for (int i = 0; i < wList.length; i++) {
	    ImagePlus imp = WindowManager.getImage(wList[i]);
	    if (imp.getStackSize() == 1)
		image[count++] = imp;
	}
	if (count < 2) {
	    IJ.error("Images to Stack", "There must be at least two open images.");
	    return;
	}
	// begin - new code of ExtendedImageToStack
	// The user defined order is limited to 10 images
	if (count > MAX_IMAGES) {
	    sortByUser = false;
	}
	// end - new code of ExtendedImageToStack
	filter = null;
	count = findMinMaxSize(count);
	boolean sizesDiffer = width != minWidth || height != minHeight;
	boolean showDialog = true;
	String macroOptions = Macro.getOptions();
	if (IJ.macroRunning() && macroOptions == null) {
	    if (sizesDiffer) {
		IJ.error("Images are not all the same size");
		return;
	    }
	    showDialog = false;
	}
	if (showDialog == true) {
	    GenericDialog gd = new GenericDialog("Images to Stack");
	    // begin - new code of ExtendedImageToStack
	    String help = "<html><h3>ExtendedImageToStack</h3><p>When combining 2 to "
		    + MAX_IMAGES
		    + " images the user can define the order.</p>"
		    + "<p>Images that are selected twice (or more often) will only be added once. The images that are not selected will then be added automaticaly.</p>"
		    + "</html>";
	    gd.addHelp(help);
	    // end - new code of ExtendedImageToStack
	    if (sizesDiffer) {
		String msg = "The " + count + " images differ in size (smallest=" + minWidth + "x" + minHeight
			+ ",\nlargest=" + maxWidth + "x" + maxHeight
			+ "). They will be converted\nto a stack using the specified method.";
		gd.setInsets(0, 0, 5);
		gd.addMessage(msg);
		gd.addChoice("Method:", methods, methods[method]);
	    }
	    gd.addStringField("Name:", name, 12);
	    gd.addStringField("Title Contains:", "", 12);
	    // begin - new code of ExtendedImageToStack
	    if (sortByUser == true) {
		gd.addMessage("User defined order:");
		// all images are identified by their titles
		String[] titles = new String[count];
		for (int i = 0; i < count; i++) {
		    titles[i] = image[i].getTitle();
		}
		// for each image a choice is added to the dialog
		for (int i = 0; i < count; i++) {
		    gd.addChoice("Z=" + (i + 1), titles, titles[i]);
		}
	    }
	    // end - new code of ExtendedImageToStack
	    if (sizesDiffer == true)
		gd.addCheckbox("Bicubic Interpolation", bicubic);
	    gd.addCheckbox("Use Titles as Labels", titlesAsLabels);
	    gd.addCheckbox("Keep Source Images", keep);
	    gd.showDialog();
	    if (gd.wasCanceled() == true)
		return;
	    if (sizesDiffer == true)
		method = gd.getNextChoiceIndex();
	    name = gd.getNextString();
	    filter = gd.getNextString();
	    // begin - new code of ExtendedImageToStack
	    // if an image is selected twice (or more often), the next image that is not selected will replace the
	    // other occurrences
	    if (sortByUser == true) {
		int[] positionAtImage = new int[count];
		boolean[] positionUsed = new boolean[count];
		boolean[] imageUsed = new boolean[count];
		Arrays.fill(positionUsed, false);
		Arrays.fill(imageUsed, false);
		ImagePlus[] temp = new ImagePlus[count];
		for (int i = 0; i < count; i++) {
		    positionAtImage[i] = gd.getNextChoiceIndex();
		    if (imageUsed[positionAtImage[i]] == false) {
			temp[i] = image[positionAtImage[i]];
			positionUsed[i] = true;
			imageUsed[positionAtImage[i]] = true;
		    }
		}
		for (int i = 0; i < count; i++) {
		    if (imageUsed[i] == false) {
			for (int j = 0; j < count; j++) {
			    if (positionUsed[j] == false) {
				temp[j] = image[i];
				positionUsed[j] = true;
				imageUsed[i] = true;
				break;
			    }
			}
		    }
		}
		image = temp;
	    }
	    // end -new code of ExtendedImageToStack
	    if (sizesDiffer == true)
		bicubic = gd.getNextBoolean();
	    titlesAsLabels = gd.getNextBoolean();
	    keep = gd.getNextBoolean();
	    if (filter != null && (filter.equals("") || filter.equals("*")))
		filter = null;
	    if (filter != null) {
		count = findMinMaxSize(count);
		if (count == 0) {
		    IJ.error("Images to Stack", "None of the images have a title containing \"" + filter + "\"");
		}
	    }
	} else
	    keep = false;
	if (method == SCALE_SMALL) {
	    width = minWidth;
	    height = minHeight;
	} else if (method == SCALE_LARGE) {
	    width = maxWidth;
	    height = maxHeight;
	}
	double min = Double.MAX_VALUE;
	double max = -Double.MAX_VALUE;
	ImageStack stack = new ImageStack(width, height);
	FileInfo fi = image[0].getOriginalFileInfo();
	if (fi != null && fi.directory == null)
	    fi = null;
	for (int i = 0; i < count; i++) {
	    ImageProcessor ip = image[i].getProcessor();
	    if (ip == null)
		break;
	    if (ip.getMin() < min)
		min = ip.getMin();
	    if (ip.getMax() > max)
		max = ip.getMax();
	    String label = titlesAsLabels ? image[i].getTitle() : null;
	    if (label != null) {
		String info = (String) image[i].getProperty("Info");
		if (info != null)
		    label += "\n" + info;
	    }
	    if (fi != null) {
		FileInfo fi2 = image[i].getOriginalFileInfo();
		if (fi2 != null && !fi.directory.equals(fi2.directory))
		    fi = null;
	    }
	    switch (stackType) {
	    case 16:
		ip = ip.convertToShort(false);
		break;
	    case 32:
		ip = ip.convertToFloat();
		break;
	    case rgb:
		ip = ip.convertToRGB();
		break;
	    default:
		break;
	    }
	    if (ip.getWidth() != width || ip.getHeight() != height) {
		switch (method) {
		case COPY_TOP_LEFT:
		case COPY_CENTER:
		    ImageProcessor ip2 = null;
		    switch (stackType) {
		    case 8:
			ip2 = new ByteProcessor(width, height);
			break;
		    case 16:
			ip2 = new ShortProcessor(width, height);
			break;
		    case 32:
			ip2 = new FloatProcessor(width, height);
			break;
		    case rgb:
			ip2 = new ColorProcessor(width, height);
			break;
		    }
		    int xoff = 0,
		    yoff = 0;
		    if (method == COPY_CENTER) {
			xoff = (width - ip.getWidth()) / 2;
			yoff = (height - ip.getHeight()) / 2;
		    }
		    ip2.insert(ip, xoff, yoff);
		    ip = ip2;
		    break;
		case SCALE_SMALL:
		case SCALE_LARGE:
		    ip.setInterpolationMethod((bicubic ? ImageProcessor.BICUBIC : ImageProcessor.BILINEAR));
		    ip.resetRoi();
		    ip = ip.resize(width, height);
		    break;
		}
	    } else if (keep)
		ip = ip.duplicate();
	    stack.addSlice(label, ip);
	    if (!keep) {
		image[i].changes = false;
		image[i].close();
	    }
	}
	if (stack.getSize() == 0)
	    return;
	ImagePlus imp = new ImagePlus(name, stack);
	if (stackType == 16 || stackType == 32)
	    imp.getProcessor().setMinAndMax(min, max);
	if (cal2 != null)
	    imp.setCalibration(cal2);
	if (fi != null) {
	    fi.fileName = "";
	    fi.nImages = imp.getStackSize();
	    imp.setFileInfo(fi);
	}
	imp.show();
    }

    final int findMinMaxSize(int count) {
	int index = 0;
	stackType = 8;
	width = 0;
	height = 0;
	cal2 = image[0].getCalibration();
	maxWidth = 0;
	maxHeight = 0;
	minWidth = Integer.MAX_VALUE;
	minHeight = Integer.MAX_VALUE;
	minSize = Integer.MAX_VALUE;
	maxSize = 0;
	for (int i = 0; i < count; i++) {
	    if (exclude(image[i].getTitle()))
		continue;
	    if (image[i].getType() == ImagePlus.COLOR_256)
		stackType = rgb;
	    int type = image[i].getBitDepth();
	    if (type == 24)
		type = rgb;
	    if (type > stackType)
		stackType = type;
	    int w = image[i].getWidth(), h = image[i].getHeight();
	    if (w > width)
		width = w;
	    if (h > height)
		height = h;
	    int size = w * h;
	    if (size < minSize) {
		minSize = size;
		minWidth = w;
		minHeight = h;
	    }
	    if (size > maxSize) {
		maxSize = size;
		maxWidth = w;
		maxHeight = h;
	    }
	    if (!image[i].getCalibration().equals(cal2))
		cal2 = null;
	    image[index++] = image[i];
	}
	return index;
    }

    final boolean exclude(String title) {
	return filter != null && title != null && title.indexOf(filter) == -1;
    }

}
