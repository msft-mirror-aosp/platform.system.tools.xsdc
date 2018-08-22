package com.android.xsdc.tag;

import com.android.xsdc.XsdParserException;

public class XsdRestriction extends XsdSimpleType {
    final private XsdType base;

    public XsdRestriction(String name, XsdType base) throws XsdParserException {
        super(name);
        if (base == null) {
            throw new XsdParserException("restriction base should exist in simpleType");
        }
        this.base = base;
    }

    public XsdType getBase() {
        return base;
    }
}
