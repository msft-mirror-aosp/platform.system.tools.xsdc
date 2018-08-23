package com.android.xsdc.tag;

import com.android.xsdc.XsdParserException;

import java.util.Collections;
import java.util.List;

public class XsdUnion extends XsdSimpleType {
    final private List<XsdType> memberTypes;

    public XsdUnion(String name, List<XsdType> memberTypes) throws XsdParserException {
        super(name);
        if (memberTypes == null || memberTypes.isEmpty()) {
            throw new XsdParserException("union memberTypes should exist in simpleType");
        }
        this.memberTypes = Collections.unmodifiableList(memberTypes);
    }

    public List<XsdType> getMemberTypes() {
        return memberTypes;
    }
}
