package com.android.xsdc.tag;

import java.util.List;

public class XsdComplexContent extends XsdComplexType {

    public XsdComplexContent(String name, XsdType base, List<XsdAttribute> attributes,
            List<XsdElement> elements) {
        super(name, base, attributes, elements);
    }
}
