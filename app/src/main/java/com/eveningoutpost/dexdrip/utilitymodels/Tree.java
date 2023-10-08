package com.eveningoutpost.dexdrip.utilitymodels;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by jamorham on 05/11/2017.
 */

public class Tree<T> {

    T data;
    private Tree<T> parent;
    private List<Tree<T>> children;

    public Tree(T data) {
        this.data = data;
        this.children = new LinkedList<>();
    }

    public Tree<T> addChild(T child) {
        Tree<T> node = new Tree<T>(child);
        node.parent = this;
        this.children.add(node);
        return node;
    }

    public List<Tree<T>> getChildren() {
        return children;
    }

    public Tree<T> getParent() {
        return parent;
    }

    public List<T> getChildElements() {
        final List<T> list = new LinkedList<>();
        for (Tree<T> node : children) {
            list.add(node.data);
        }
        return list;
    }

}