package org.objectstyle.cayenne.exp;
/* ====================================================================
 * 
 * The ObjectStyle Group Software License, Version 1.0 
 *
 * Copyright (c) 2002 The ObjectStyle Group 
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

import junit.framework.*;
import java.util.logging.*;
import java.util.*;


public class TernaryExpressionTst extends TestCase {
    // non-existent type
    private static final int defaultType = -37;
    protected TernaryExpression expr;
    
    public TernaryExpressionTst(String name) {
        super(name);
    }
    
    
    protected void setUp() throws java.lang.Exception {
        expr = new TernaryExpression(defaultType);
    }
    
    
    public void testGetType() throws java.lang.Exception {
        assertEquals(defaultType, expr.getType());
    }
    
    
    public void testGetOperandCount() throws java.lang.Exception {
        assertEquals(3, expr.getOperandCount());
    }
    
    
    public void testGetOperandAtIndex() throws java.lang.Exception {
        expr.getOperand(0);
        expr.getOperand(1);
        expr.getOperand(2);
        
        try {
            expr.getOperand(3);
            fail();
        }
        catch(Exception ex) {
            // exception expected..
        }
    }
    
    
    public void testSetOperandAtIndex() throws java.lang.Exception {
        Object o1 = new Object();
        Object o2 = new Object();
        Object o3 = new Object();
        
        expr.setOperand(0, o1);
        expr.setOperand(1, o2);
        expr.setOperand(2, o3);
        assertSame(o1, expr.getOperand(0));
        assertSame(o2, expr.getOperand(1));  
        assertSame(o3, expr.getOperand(2));  
        
        try {
            expr.setOperand(3, o1);
            fail();
        }
        catch(Exception ex) {
            // exception expected..
        }
    }
    
}
