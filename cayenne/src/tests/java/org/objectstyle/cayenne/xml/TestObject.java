package org.objectstyle.cayenne.xml;

/**
 * @author Andrei Adamchik
 */
public class TestObject {

    protected String name;
    protected int age;
    protected boolean open;

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }
}