package com.lemnik.anrtrace;

import static java.lang.Math.min;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

class SampleTreeNode {

    /*
     * Implementation Details:
     *
     * The SampleTreeNode class is a mixture of different possible implementations depending on it's
     * actual state. By default the objects are "leaf" nodes and in this state `children` is `null`,
     * indicating that they were sampled at the top of the stack.
     *
     * ## Single Child Frames
     * Many stack frames captured during sampling will only ever have a single child (these typically
     * appear near the bottom of the stack, and are associated with Thread.run and similar framework
     * frames). For these SampleTreeNodes the `children` will be a direct reference to its *one*
     * child node, avoiding the additional indirection of an array.
     *
     * ## Multi Child Frames
     * One a SampleTreeNode is used to track frames with more than one child (methods that have been
     * seen calling more than one other method), the `children` field is converted into a linear
     * probing hashtable. This is stored as a simple array of SampleTreeNode objects, since each
     * SampleTreeNode can be used as both key and value.
     */

    private static final int MAX_PROBE_DISTANCE = 8;

    /**
     * Class name of the stack frame - must be captured directly from the VM context so that
     * it is `intern`ed and will == other `className`s
     */
    final String className;
    /**
     * Method name of the stack frame - must be captured directly from the VM context so that
     * it is `intern`ed and will == other `methodName`s
     */
    final String methodName;

    /**
     * Cached hashCode generated in the constructor by [hashCodeFor].
     */
    private final int hash;

    /**
     * One of 3 possible structures:
     * 1) null             - it's default state, there are no child frames
     * 1) SampleTreeNode   - only one child frame has been seen
     * 3) SampleTreeNode[] - internally managed "linear-probing hashtable" structure
     */
    private Object children = null;

    long counter = 0;

    long totalTimeNs = 0;

    SampleTreeNode(@NonNull String className, @NonNull String methodName) {
        this.className = className.intern();
        this.methodName = methodName.intern();
        this.hash = hashCodeFor(className, methodName);
    }

    SampleTreeNode child(String className, String methodName) {
        if (children == null) {
            SampleTreeNode child = new SampleTreeNode(className, methodName);
            children = child;
            return child;
        } else if (children instanceof SampleTreeNode) {
            SampleTreeNode child = (SampleTreeNode) children;
            if (child.className.equals(className) && child.methodName.equals(methodName)) {
                return child;
            } else {
                SampleTreeNode newChild = new SampleTreeNode(className, methodName);
                // start the table at size 2
                SampleTreeNode[] table = new SampleTreeNode[2];
                tableInsert(table, child);
                tableInsert(table, newChild);

                children = table;
                return newChild;
            }
        } else {
            SampleTreeNode[] table = (SampleTreeNode[]) children;
            SampleTreeNode child = tableLookup(table, className, methodName);

            if (child == null) {
                child = new SampleTreeNode(className, methodName);
                while (!tableInsert(table, child)) {
                    // double the size of the table and rehash everything
                    table = tableRehash(table);
                    children = table;
                }

            }

            return child;
        }
    }

    static SampleTreeNode tableLookup(SampleTreeNode[] table, String className, String methodName) {
        final int mask = table.length - 1;
        final int hashCode = hashCodeFor(className, methodName);
        int index = hashCode & mask;

        SampleTreeNode node = table[index];
        if (node == null) {
            // nothing in this slot == not in the table
            return null;
        }

        if (node.hash == hashCode &&
                // we have a hash match, check the content
                node.className.equals(className) && node.methodName.equals(methodName)) {
            return node;
        }

        final int probeEnd = min(table.length - 1, MAX_PROBE_DISTANCE);
        for (int i = 0; i < probeEnd; i++) {
            index = (index + 1) & mask;
            node = table[index];

            // bumped into a null slot - exit
            if (node == null) {
                return null;
            } else if (node.hash == hashCode &&
                    // we have a hash match, check the content
                    node.className.equals(className) && node.methodName.equals(methodName)) {

                return node; // match found!
            } else if ((node.hash & mask) == index) {
                // this node is "in place" - so we've reached the end of a probe line
                return null;
            }
        }

        return null;
    }

    static boolean tableInsert(SampleTreeNode[] table, SampleTreeNode newChild) {
        final int mask = table.length - 1;
        int index = newChild.hash & mask;

        if (table[index] == null) {
            table[index] = newChild;
            return true;
        } else {
            // we have a slot collision

            // first check that the current occupant is slotted "correctly" (ie: not overspill)
            SampleTreeNode current = table[index];
            if ((current.hash & mask) == index) {
                final int probeEnd = min(table.length - 1, MAX_PROBE_DISTANCE);
                for (int i = 0; i < probeEnd; i++) {
                    index = (index + 1) & mask;
                    current = table[index];

                    if (current == null) {
                        // we found an available slot - and we occupy it
                        table[index] = newChild;
                        return true;
                    } else if ((current.hash & mask) == index) {
                        // not enough space - grow the table
                        return false;
                    } // else { continue search }
                }

            }

            // no available slot could be found within the allowed probe space - grow the table
            return false;
        }
    }

    static SampleTreeNode[] tableRehash(SampleTreeNode[] existing) {
        SampleTreeNode[] newTable = new SampleTreeNode[existing.length << 1];

        for (SampleTreeNode node : existing) {
            if (node != null) {
                tableInsert(newTable, node);
            }
        }

        return newTable;
    }

    public boolean isNotEmpty() {
        return children != null;
    }

    <E> void accept(@NonNull StackTreeVisitor<E> walker, @Nullable E parent) {
        Object c = children;
        if (c == null) {
            walker.visitLeaf(className, methodName, counter, totalTimeNs, parent);
        } else if (c instanceof SampleTreeNode) {
            E token = walker.openBranch(className, methodName, counter, totalTimeNs, parent);
            ((SampleTreeNode) c).accept(walker, token);
            walker.closeBranch(token, parent);
        } else {
            SampleTreeNode[] table = (SampleTreeNode[]) c;
            E token = walker.openBranch(className, methodName, counter, totalTimeNs, parent);
            for (SampleTreeNode node : table) {
                if (node != null) {
                    node.accept(walker, token);
                }
            }

            walker.closeBranch(token, parent);
        }
    }

    private static int hashCodeFor(@NonNull String className, @NonNull String methodName) {
        return className.hashCode() ^ methodName.hashCode();
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }

        if (other == this) {
            return true;
        }

        SampleTreeNode node = (SampleTreeNode) other;
        // ignore lint warnings - these strings are interned
        return node.className.equals(className) && node.methodName.equals(methodName);
    }

    @Override
    public String toString() {
        return className + ':' + methodName;
    }
}
