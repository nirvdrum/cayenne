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
import javax.swing.border.*;
import javax.swing.plaf.UIResource;
import javax.swing.UIManager;

/**
 *
 * @author Andriy Shapochka
 * @version 1.0
 */

public class CustomBorderFactory {
  public static final Border THIN_RAISED_BORDER = new ThinRaisedBorder();
  public static final Border THIN_LOWERED_BORDER = new ThinLoweredBorder();
  public static final Border TILE_BORDER = new TileBorder();

  public static class ThinRaisedBorder extends AbstractBorder {
    private static final Insets INSETS = new Insets(2, 2, 2, 2);

    public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
      g.translate(x, y);
      g.setColor(c.getBackground().brighter());
      g.drawLine(0, 0, w - 2, 0);
      g.drawLine(0, 0, 0, h - 2);
      g.setColor(c.getBackground().darker());
      g.drawLine(w - 1, 0, w - 1, h - 1);
      g.drawLine(0, h - 1, w - 1, h - 1);
      g.translate(-x, -y);
    }

    public Insets getBorderInsets(Component c) { return INSETS; }
  }


  public static class ThinLoweredBorder extends AbstractBorder {
    private static final Insets INSETS = new Insets(2, 2, 2, 2);

    public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
      g.translate(x, y);
      g.setColor(c.getBackground().darker());
      g.drawLine(0, 0, w - 2, 0);
      g.drawLine(0, 0, 0, h - 2);
      g.setColor(c.getBackground().brighter());
      g.drawLine(w - 1, 0, w - 1, h - 1);
      g.drawLine(0, h - 1, w - 1, h - 1);
      g.translate(-x, -y);
    }

    public Insets getBorderInsets(Component c) { return INSETS; }
  }

  public static class TileBorder extends AbstractBorder implements UIResource {

    public static final Insets INSETS	= new Insets(1, 1, 3, 3);

    static final int   ALPHA1			= 173;
    static final int   ALPHA2			=  66;


    public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
      paintShadowedBorder(c, g, x, y, w, h);
    }

    private void paintShadowedBorder(Component c, Graphics g, int x, int y, int w, int h) {
      Color background	= c.getParent().getBackground();
      Color highlight		= UIManager.getColor("controlLtHighlight");
      Color shadow    = UIManager.getColor("controlShadow");
      Color lightShadow   = new Color(shadow.getRed(),
                                      shadow.getGreen(),
                                      shadow.getBlue(),
                                      ALPHA1);
      Color lighterShadow = new Color(shadow.getRed(),
                                      shadow.getGreen(),
                                      shadow.getBlue(),
                                      ALPHA2);
      g.translate(x, y);
      // Dark border
      g.setColor(shadow);
      g.drawRect(0,   0, w-3, h-3);
      // Paint background before painting the shadow
      g.setColor(background);
      g.fillRect(w - 2, 0, 2, h);
      g.fillRect(0, h-2, w, 2);
      // Shadow line 1
      g.setColor(lightShadow);
      g.drawLine(w - 2, 1, w - 2, h - 2);
      g.drawLine(1, h - 2, w - 3, h - 2);
      // Shadow line2
      g.setColor(lighterShadow);
      g.drawLine(w - 1, 2, w - 1, h - 2);
      g.drawLine(2, h - 1, w - 2, h - 1);
      g.translate(-x, -y);
    }

    public Insets getBorderInsets(Component c) {
      return INSETS;
    }
  }
}