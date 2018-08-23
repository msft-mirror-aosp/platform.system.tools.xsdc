package com.android.xsdc.tag;

import javax.xml.namespace.QName;

public abstract class XsdTag {
    final private String name;
    final private QName ref;

    XsdTag(String name, QName ref) {
        this.name = name;
        this.ref = ref;
    }

    public String getName() {
        return name;
    }

    public QName getRef() {
        return ref;
    }
}
