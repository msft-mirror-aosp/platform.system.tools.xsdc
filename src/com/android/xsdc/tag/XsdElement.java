package com.android.xsdc.tag;

import com.android.xsdc.XsdParserException;

import javax.xml.namespace.QName;

public class XsdElement extends XsdTag {
    final private XsdType type;
    final private boolean multiple;

    public XsdElement(String name, QName ref, XsdType type, boolean multiple)
            throws XsdParserException {
        super(name, ref);
        if (name == null && ref == null) {
            throw new XsdParserException("name and ref cannot be both null");
        }
        if (name != null && type == null) {
            throw new XsdParserException(
                    String.format("In element '%s', type definition should exist", name));
        }
        this.type = type;
        this.multiple = multiple;
    }

    public XsdType getType() {
        return type;
    }

    public boolean isMultiple() {
        return multiple;
    }
}
