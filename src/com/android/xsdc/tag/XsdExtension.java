package com.android.xsdc.tag;

import java.util.ArrayList;
import java.util.List;

public class XsdExtension extends XsdType {
    private XsdTypeReferrer base;
    private List<XsdAttribute> attributes = new ArrayList<>();
    private List<XsdElement> elements = new ArrayList<>();

    public XsdExtension(String name, XsdTypeReferrer base) {
        super(name);
        this.base = base;
    }

    public XsdTypeReferrer getBase() {
        return base;
    }

    public List<XsdAttribute> getAttributes() {
        return attributes;
    }

    public List<XsdElement> getElements() {
        return elements;
    }
}
