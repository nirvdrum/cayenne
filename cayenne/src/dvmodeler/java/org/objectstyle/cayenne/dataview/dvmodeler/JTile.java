/* ====================================================================
 *
 * The ObjectStyle Group Software License, Version 1.0
 *
 * Copyright (c) 2002-2004 The ObjectStyle Group
 * and individual authors of the software.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        ObjectStyle Group (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "ObjectStyle Group" and "Cayenne"
 *    must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact andrus@objectstyle.org.
 *
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    nor may "ObjectStyle" appear in their names without prior written
 *    permission of the ObjectStyle Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the ObjectStyle Group.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 *
 */

package org.objectstyle.cayenne.dataview.dvmodeler;

import java.awt.*;
import javax.swing.*;
import com.jgoodies.forms.factories.*;
import com.jgoodies.plaf.plastic.*;

/**
 *
 * @author Andriy Shapochka
 * @version 1.0
 */

public class JTile extends JPanel {
  private TitleBar titleBar = new TitleBar();
  private JComponent contentPane;
  private String captionText;
  private Icon captionIcon;

  public JTile() {
    this(null, null, null);
  }

  public JTile(JComponent contentPane, String caption) {
    this(contentPane, caption, null);
  }

  public JTile(String caption) {
    this(null, caption, null);
  }

  public JTile(JComponent contentPane) {
    this(contentPane, null, null);
  }

  public JTile(JComponent contentPane, String caption, Icon icon) {
    init();
    setContentPane(contentPane);
    setCaption(caption);
    setIcon(icon);
  }

  public void setContentPane(JComponent contentPane) {
    this.contentPane = contentPane;
    this.add(contentPane, BorderLayout.CENTER);
  }

  public JComponent getContentPane() {
    return contentPane;
  }

  public void setCaption(String text) {
    this.captionText = text;
    titleBar.setCaption(text);
  }

  public String getCaption() {
    return captionText;
  }

  public void setIcon(Icon icon) {
    this.captionIcon = icon;
    titleBar.setIcon(icon);
  }

  public Icon getIcon() {
    return captionIcon;
  }

  private void init() {
    this.setBorder(CustomBorderFactory.TILE_BORDER);
    this.setLayout(new BorderLayout());
    this.add(titleBar, BorderLayout.NORTH);
  }

  private class TitleBar extends JPanel {
    private Color leftColor;
    private JLabel caption = new JLabel(" ");
    private JLabel captionIcon = new JLabel(EmptyIcon.DEFAULT_ICON);

    private TitleBar() {
      leftColor = PlasticXPLookAndFeel.getSimpleInternalFrameBackground();
      caption.setOpaque(false);
      caption.setForeground(PlasticXPLookAndFeel.getSimpleInternalFrameForeground());
      caption.setBorder(Borders.DLU2_BORDER);
      captionIcon.setOpaque(false);
      captionIcon.setBorder(Borders.DLU2_BORDER);
      setLayout(new BorderLayout(0, 0));
      add(caption, BorderLayout.CENTER);
      add(captionIcon, BorderLayout.WEST);
    }

    private void setCaption(String text) {
      caption.setText((text != null && text.length() > 0 ? text : " "));
    }

    private void setIcon(Icon icon) {
      captionIcon.setIcon((icon != null ? icon : EmptyIcon.DEFAULT_ICON));
    }

    public void paintComponent(Graphics g) {
      super.paintComponent(g);
      Graphics2D g2d = (Graphics2D)g;
      Paint oldPaint = g2d.getPaint();
      int width = getWidth();
      int height = getHeight();
      Color highlight = UIManager.getColor("controlLtHighlight");
      Color shadow    = UIManager.getColor("controlShadow");

      g.setColor(highlight);
      g.drawLine(0, 0, width, 0);
      g.drawLine(0, 0, 0, height);
      g.setColor(shadow);
      g.drawLine(0, height-1, width, height-1);
      GradientPaint paint = new GradientPaint(
          1, height/2, leftColor, width, height/2, getBackground(), false);
      g2d.setPaint(paint);
      g2d.fillRect(1, 1, width-1, height-2);

      g2d.setPaint(oldPaint);
    }
  }
}
