package com.android.xsdc.tag;

import com.android.xsdc.XsdParserException;

public class XsdList extends XsdSimpleType {
    final private XsdType itemType;

    public XsdList(String name, XsdType itemType) throws XsdParserException {
        super(name);
        if (itemType == null) {
            throw new XsdParserException("list itemType should exist in simpleType");
        }
        this.itemType = itemType;
    }

    public XsdType getItemType() {
        return itemType;
    }
}
