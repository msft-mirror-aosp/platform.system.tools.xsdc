package com.android.xsdc;

import com.android.xsdc.tag.*;

import java.util.Collections;
import java.util.Map;

public class XmlSchema {
    final private Map<String, XsdElement> elementMap;
    final private Map<String, XsdType> typeMap;
    final private Map<String, XsdAttribute> attributeMap;

    XmlSchema(Map<String, XsdElement> elementMap, Map<String, XsdType> typeMap,
            Map<String, XsdAttribute> attributeMap) {
        this.elementMap = Collections.unmodifiableMap(elementMap);
        this.typeMap = Collections.unmodifiableMap(typeMap);
        this.attributeMap = Collections.unmodifiableMap(attributeMap);
    }

    public Map<String, XsdElement> getElementMap() {
        return elementMap;
    }

    public Map<String, XsdType> getTypeMap() {
        return typeMap;
    }

    public Map<String, XsdAttribute> getAttributeMap() {
        return attributeMap;
    }
}
