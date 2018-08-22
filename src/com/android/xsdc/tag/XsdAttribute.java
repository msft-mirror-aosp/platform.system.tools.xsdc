package com.android.xsdc.tag;

import com.android.xsdc.XsdParserException;

import javax.xml.namespace.QName;

public class XsdAttribute extends XsdTag {
    final private XsdType type;

    public XsdAttribute(String name, QName ref, XsdType type)
            throws XsdParserException {
        super(name, ref);
        if (name == null && ref == null) {
            throw new XsdParserException("name and ref cannot be both null");
        }
        if (ref == null && type == null) {
            throw new XsdParserException("type definition should exist");
        }
        this.type = type;
    }

    public XsdType getType() {
        return type;
    }
}
