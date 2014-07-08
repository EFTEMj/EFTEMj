/**
 * EFTEMj - Processing of Energy Filtering TEM images with ImageJ
 * 
 * Copyright (c) 2013, Michael Epping
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
package gui;

import java.awt.Button;
import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import ij.gui.MultiLineLabel;
import ij.gui.WaitForUserDialog;

/**
 * This is an extended version of {@link WaitForUserDialog}. I need to insert a new {@link Component} between the
 * {@link MultiLineLabel} and the Ok {@link Button}. The functionality of {@link WaitForUserDialog} is preserved.
 * 
 * I implement {@link ContainerListener} to add the possibility to close the dialog by pressing [Esc]. I found this
 * method at http://www.devx.com/tips/Tip/13547 (22.04.2013).
 * 
 * @author Michael Entrup b. Epping <entrup@arcor.de>
 * 
 */
@SuppressWarnings("serial")
public class ExtendedWaitForUserDialog extends WaitForUserDialog implements ContainerListener {

    private static String hint = "\n \nPress [Esc] to abort.";
    private boolean escPressed = false;

    /**
     * The constructor modifies the dialog to show an additional component.
     * 
     * @param title
     * @param text
     *            A text to describe the task the user has to perform.
     * @param container
     *            An additional {@link Component} (e.g. a {@link Button}) that is placed at the
     *            {@link ExtendedWaitForUserDialog}.
     */
    public ExtendedWaitForUserDialog(String title, String text, Component container) {
	super(title, text + hint);
	addKeyAndContainerListenerRecursively(this);
	if (container == null)
	    return;
	GridBagConstraints c = new GridBagConstraints();
	// The ok-button has to be removed in order to place the new component at gridy = 2.
	Component obj = getComponent(1);
	remove(obj);
	c.insets = new Insets(6, 6, 0, 6);
	c.gridx = 0;
	c.gridy = 2;
	c.anchor = GridBagConstraints.WEST;
	add(container, c);
	c.insets = new Insets(2, 6, 6, 6);
	c.gridx = 0;
	c.gridy = 3;
	c.anchor = GridBagConstraints.EAST;
	add(obj, c);
	pack();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ij.gui.WaitForUserDialog#escPressed()
     */
    public boolean escPressed() {
	return super.escPressed() | escPressed;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.ContainerListener#componentAdded(java.awt.event.ContainerEvent)
     */
    @Override
    public void componentAdded(ContainerEvent e) {
	addKeyAndContainerListenerRecursively(e.getChild());
    }

    /**
     * This method adds a {@link KeyListener} to all added components. This is done recursively.
     * 
     * @param c
     *            The {@link Component} that is added.
     */
    private void addKeyAndContainerListenerRecursively(Component c) {
	c.addKeyListener(this);
	if (c instanceof Container) {
	    Container cont = (Container) c;
	    cont.addContainerListener(this);
	    Component[] children = cont.getComponents();
	    for (int i = 0; i < children.length; i++) {
		addKeyAndContainerListenerRecursively(children[i]);
	    }
	}
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.ContainerListener#componentRemoved(java.awt.event.ContainerEvent)
     */
    @Override
    public void componentRemoved(ContainerEvent e) {
	removeKeyAndContainerListenerRecursively(e.getChild());
    }

    /**
     * This method removes the {@link KeyListener} from all removed components. This is done recursively.
     * 
     * @param c
     *            The {@link Component} that is removed.
     */
    private void removeKeyAndContainerListenerRecursively(Component c) {
	c.removeKeyListener(this);
	if (c instanceof Container) {
	    Container cont = (Container) c;
	    cont.removeContainerListener(this);
	    Component[] children = cont.getComponents();
	    for (int i = 0; i < children.length; i++) {
		removeKeyAndContainerListenerRecursively(children[i]);
	    }
	}
    }

    /*
     * (non-Javadoc)
     * 
     * @see ij.gui.WaitForUserDialog#keyPressed(java.awt.event.KeyEvent)
     */
    @Override
    public void keyPressed(KeyEvent e) {
	int code = e.getKeyCode();
	if (code == KeyEvent.VK_ESCAPE) {
	    escPressed = true;
	    close();
	} else if (code == KeyEvent.VK_ENTER) {
	    close();
	}
    }

    /*
     * (non-Javadoc)
     * 
     * @see ij.gui.WaitForUserDialog#keyReleased(java.awt.event.KeyEvent)
     */
    @Override
    public void keyReleased(KeyEvent e) {
    }

    /*
     * (non-Javadoc)
     * 
     * @see ij.gui.WaitForUserDialog#keyTyped(java.awt.event.KeyEvent)
     */
    @Override
    public void keyTyped(KeyEvent e) {
    }
}
