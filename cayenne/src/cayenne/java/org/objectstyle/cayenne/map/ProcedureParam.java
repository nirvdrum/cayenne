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
package org.objectstyle.cayenne.map;

/**
 * A descriptor for the StoredProcedure parameter.
 * 
 * @author Andrei Adamchik
 */
public class ProcedureParam extends DbAttribute {

    public static final int IN_PARAM = 1;
    public static final int OUT_PARAM = 2;
    public static final int IN_OUT_PARAM = 3;

    /** 
     * Defines a stored procedure parameter with unknown direction.
     */
    public static final int VOID_PARAM = 4;

    protected int direction = VOID_PARAM;
    protected boolean returned;

    /**
     * Constructor for ProcedureParam.
     */
    public ProcedureParam() {
        super();
    }

    public ProcedureParam(String name) {
        super(name);
    }

    public ProcedureParam(
        String name,
        int type,
        int direction) {
        super(name);
        setType(type);
        setDirection(direction);
    }

    /**
     * Throws an exception if the entity is not a Procedure.
     */
    public void setEntity(Entity entity) {
        if (entity != null && !(entity instanceof Procedure)) {
            throw new IllegalArgumentException("Only Procedure can be a parent of ProcedureParam.");
        }

        super.setEntity(entity);
    }

    /**
     * Returns the direction.
     * @return int
     */
    public int getDirection() {
        return direction;
    }

    /**
     * Sets the direction.
     * @param direction The direction to set
     */
    public void setDirection(int direction) {
        if (direction != IN_PARAM
            && direction != OUT_PARAM
            && direction != IN_OUT_PARAM
            && direction != VOID_PARAM) {
            throw new IllegalArgumentException(
                "Unknown parameter type: " + direction);
        }

        this.direction = direction;
    }
    
    /**
     * Returns the returned.
     * @return boolean
     */
    public boolean isReturned() {
        return returned;
    }

    /**
     * Sets the returned.
     * @param returned The returned to set
     */
    public void setReturned(boolean returned) {
        this.returned = returned;
    }
}
