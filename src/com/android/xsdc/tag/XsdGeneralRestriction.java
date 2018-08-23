package com.android.xsdc.tag;

import java.util.Collections;
import java.util.List;

public class XsdGeneralRestriction extends XsdTag {
    final private XsdType base;
    final private List<XsdAttribute> attributes;
    final private List<XsdElement> elements;

    public XsdGeneralRestriction(XsdType base, List<XsdAttribute> attributes,
            List<XsdElement> elements) {
        super(null, null);
        this.base = base;
        this.attributes = Collections.unmodifiableList(attributes);
        this.elements = Collections.unmodifiableList(elements);
    }

    public XsdType getBase() {
        return base;
    }

    public List<XsdAttribute> getAttributes() {
        return attributes;
    }

    public List<XsdElement> getElements() {
        return elements;
    }
}
