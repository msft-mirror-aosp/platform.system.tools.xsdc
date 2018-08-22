package com.android.xsdc.tag;

import java.util.List;

public class XsdSimpleContent extends XsdComplexType {
    public XsdSimpleContent(String name, XsdType base, List<XsdAttribute> attributes) {
        super(name, base, attributes, null);
    }
}
