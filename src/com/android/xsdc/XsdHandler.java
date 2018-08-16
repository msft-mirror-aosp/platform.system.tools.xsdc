package com.android.xsdc;

import com.android.xsdc.tag.*;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;

public class XsdHandler extends DefaultHandler {
    private static class State {
        final String name;
        final Map<String, String> attributeMap;
        final List<XsdTag> tags;

        State(String name, Map<String, String> attributeMap) {
            this.name = name;
            this.attributeMap = Collections.unmodifiableMap(attributeMap);
            tags = new ArrayList<>();
        }
    }

    private XmlSchema schema;
    private final Stack<State> stateStack;
    private final Map<String, String> namespaces;
    private Locator locator;

    public XsdHandler() {
        stateStack = new Stack<>();
        namespaces = new HashMap<>();
    }

    public XmlSchema getSchema() {
        return schema;
    }

    @Override
    public void setDocumentLocator(Locator locator) {
        this.locator = locator;
    }

    @Override
    public void startPrefixMapping(String prefix, String uri) {
        namespaces.put(prefix, uri);
    }

    @Override
    public void endPrefixMapping(String prefix) {
        namespaces.remove(prefix);
    }

    private QName parseQName(String str) throws XsdParserException {
        if (str == null) return null;
        String[] parsed = str.split(":");
        if (parsed.length == 2) {
            return new QName(namespaces.get(parsed[0]), parsed[1]);
        } else if (parsed.length == 1) {
            return new QName(null, str);
        }
        throw new XsdParserException(String.format("QName parse error : %s", str));
    }

    private List<QName> parseQNames(String str) throws XsdParserException {
        List<QName> qNames = new ArrayList<>();
        if (str == null) return qNames;
        String[] parsed = str.split("\\s+");
        for (String s : parsed) {
            qNames.add(parseQName(s));
        }
        return qNames;
    }

    @Override
    public void startElement(
            String uri, String localName, String qName, Attributes attributes) {
        // we need to copy attributes because it is mutable..
        Map<String, String> attributeMap = new HashMap<>();
        for (int i = 0; i < attributes.getLength(); ++i) {
            attributeMap.put(attributes.getLocalName(i), attributes.getValue(i));
        }
        stateStack.push(new State(localName, attributeMap));
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        try {
            State state = stateStack.pop();
            switch (state.name) {
                case "schema":
                    schema = makeSchema(state.attributeMap, state.tags);
                    break;
                case "element":
                    stateStack.peek().tags.add(makeElement(state.attributeMap, state.tags));
                    break;
                case "attribute":
                    stateStack.peek().tags.add(makeAttribute(state.attributeMap, state.tags));
                    break;
                case "complexType":
                    stateStack.peek().tags.add(makeComplexType(state.attributeMap, state.tags));
                    break;
                case "complexContent":
                    stateStack.peek().tags.add(makeComplexContent(state.attributeMap, state.tags));
                    break;
                case "simpleContent":
                    stateStack.peek().tags.add(makeSimpleContent(state.attributeMap, state.tags));
                    break;
                case "restriction":
                    stateStack.peek().tags.add(
                            makeGeneralRestriction(state.attributeMap, state.tags));
                    break;
                case "extension":
                    stateStack.peek().tags.add(
                            makeGeneralExtension(state.attributeMap, state.tags));
                    break;
                case "simpleType":
                    stateStack.peek().tags.add(makeSimpleType(state.attributeMap, state.tags));
                    break;
                case "list":
                    stateStack.peek().tags.add(makeSimpleTypeList(state.attributeMap, state.tags));
                    break;
                case "union":
                    stateStack.peek().tags.add(makeSimpleTypeUnion(state.attributeMap, state.tags));
                    break;
                case "sequence":
                    stateStack.peek().tags.addAll(makeSequence(state.attributeMap, state.tags));
                    break;
            }
        } catch (XsdParserException e) {
            throw new SAXException(
                    String.format("Line %d, Column %d",
                            locator.getLineNumber(), locator.getColumnNumber()), e);
        }
    }

    private XmlSchema makeSchema(Map<String, String> attributeMap, List<XsdTag> tags)
            throws XsdParserException {
        String targetNameSpace = attributeMap.get("targetNamespace");
        XmlSchema schema = new XmlSchema(targetNameSpace);
        for (XsdTag tag : tags) {
            if (tag == null) continue;
            if (tag instanceof XsdElement) {
                schema.registerElement((XsdElement) tag);
            } else if (tag instanceof XsdAttribute) {
                schema.registerAttribute((XsdAttribute) tag);
            } else if (tag instanceof XsdType) {
                schema.registerType((XsdType) tag);
            }
        }
        return schema;
    }

    private XsdElement makeElement(Map<String, String> attributeMap, List<XsdTag> tags)
            throws XsdParserException {
        String name = attributeMap.get("name");
        QName typename = parseQName(attributeMap.get("type"));
        QName ref = parseQName(attributeMap.get("ref"));

        String minOccurs = attributeMap.get("minOccurs");
        String maxOccurs = attributeMap.get("maxOccurs");
        boolean nullable = false, multiple = false;
        if (maxOccurs != null) {
            if (maxOccurs.equals("0")) return null;
            if (maxOccurs.equals("unbounded") || Integer.parseInt(maxOccurs) > 1) multiple = true;
        }
        if (minOccurs != null) {
            if (minOccurs.equals("0")) nullable = true;
        }

        XsdElement element;
        if (ref != null) {
            element = new XsdElement(ref, nullable, multiple);
        } else if (typename != null) {
            element = new XsdElement(name, new XsdTypeReferrer(typename), nullable, multiple);
        } else {
            XsdType type = null;
            for (XsdTag tag : tags) {
                if (tag == null) continue;
                if (tag instanceof XsdType) {
                    type = (XsdType) tag;
                }
            }
            element = new XsdElement(name, new XsdTypeReferrer(type), nullable, multiple);
        }
        return element;
    }

    private XsdAttribute makeAttribute(Map<String, String> attributeMap, List<XsdTag> tags)
            throws XsdParserException {
        String name = attributeMap.get("name");
        QName typename = parseQName(attributeMap.get("type"));
        QName ref = parseQName(attributeMap.get("ref"));

        String use = attributeMap.get("use");
        boolean nullable = true;
        if (use != null) {
            if (use.equals("prohibited")) return null;
            if (use.equals("required")) nullable = false;
        }

        XsdAttribute attribute;
        if (ref != null) {
            attribute = new XsdAttribute(ref, nullable);
        } else if (typename != null) {
            attribute = new XsdAttribute(name, new XsdTypeReferrer(typename), nullable);
        } else {
            XsdType type = null;
            for (XsdTag tag : tags) {
                if (tag == null) continue;
                if (tag instanceof XsdType) {
                    type = (XsdType) tag;
                }
            }
            attribute = new XsdAttribute(name, new XsdTypeReferrer(type), nullable);
        }
        return attribute;
    }

    private XsdComplexType makeComplexType(Map<String, String> attributeMap, List<XsdTag> tags)
            throws XsdParserException {
        String name = attributeMap.get("name");
        XsdComplexType type = null;
        XsdComplexContent content = new XsdComplexContent(name, null);
        for (XsdTag tag : tags) {
            if (tag == null) continue;
            if (tag instanceof XsdAttribute) {
                content.getAttributes().add((XsdAttribute) tag);
            } else if (tag instanceof XsdElement) {
                content.getElements().add((XsdElement) tag);
            } else if (tag instanceof XsdComplexContent) {
                XsdComplexContent child = (XsdComplexContent) tag;
                XsdComplexContent complexContent = new XsdComplexContent(name, child.getBase());
                complexContent.getElements().addAll(child.getElements());
                complexContent.getAttributes().addAll(child.getAttributes());
                type = complexContent;
            } else if (tag instanceof XsdSimpleContent) {
                XsdSimpleContent child = (XsdSimpleContent) tag;
                type = new XsdSimpleContent(name, child.getBase());
                type.getAttributes().addAll(child.getAttributes());
            }
        }
        return (type != null) ? type : content;
    }

    private XsdComplexContent makeComplexContent(Map<String, String> attributeMap,
            List<XsdTag> tags) throws XsdParserException {
        XsdComplexContent content = null;
        for (XsdTag tag : tags) {
            if (tag == null) continue;
            if (tag instanceof XsdExtension) {
                XsdExtension extension = (XsdExtension) tag;
                content = new XsdComplexContent(null, extension.getBase());
                content.getElements().addAll(extension.getElements());
                content.getAttributes().addAll(extension.getAttributes());
            } else if (tag instanceof XsdRestriction) {
                XsdRestriction restriction = (XsdRestriction) tag;
                content = new XsdComplexContent(null, restriction.getBase());
            }
        }
        return content;
    }

    private XsdSimpleContent makeSimpleContent(Map<String, String> attributeMap, List<XsdTag> tags)
            throws XsdParserException {
        XsdSimpleContent content = null;
        for (XsdTag tag : tags) {
            if (tag == null) continue;
            if (tag instanceof XsdExtension) {
                XsdExtension extension = (XsdExtension) tag;
                content = new XsdSimpleContent(null, extension.getBase());
                content.getAttributes().addAll(extension.getAttributes());
            } else if (tag instanceof XsdRestriction) {
                XsdRestriction restriction = (XsdRestriction) tag;
                content = new XsdSimpleContent(null, restriction.getBase());
            }
        }
        return content;
    }

    private XsdRestriction makeGeneralRestriction(Map<String, String> attributeMap,
            List<XsdTag> tags) throws XsdParserException {
        // although class XsdRestriction is derived from XsdSimpleType, it is also used for
        // complex types.
        QName base = parseQName(attributeMap.get("base"));

        XsdRestriction restriction;
        if (base != null) {
            restriction = new XsdRestriction(null, new XsdTypeReferrer(base));
        } else {
            XsdType type = null;
            for (XsdTag tag : tags) {
                if (tag == null) continue;
                if (tag instanceof XsdType) {
                    type = (XsdType) tag;
                }
            }
            restriction = new XsdRestriction(null, new XsdTypeReferrer(type));
        }
        return restriction;
    }

    private XsdExtension makeGeneralExtension(Map<String, String> attributeMap, List<XsdTag> tags)
            throws XsdParserException {
        QName base = parseQName(attributeMap.get("base"));

        XsdExtension extension = new XsdExtension(null, new XsdTypeReferrer(base));
        for (XsdTag tag : tags) {
            if (tag == null) continue;
            if (tag instanceof XsdAttribute) {
                extension.getAttributes().add((XsdAttribute) tag);
            } else if (tag instanceof XsdElement) {
                extension.getElements().add((XsdElement) tag);
            }
        }
        return extension;
    }

    private XsdSimpleType makeSimpleType(Map<String, String> attributeMap, List<XsdTag> tags)
            throws XsdParserException {
        String name = attributeMap.get("name");
        XsdSimpleType type = null;
        for (XsdTag tag : tags) {
            if (tag == null) continue;
            if (tag instanceof XsdList) {
                type = new XsdList(name, ((XsdList) tag).getItemType());
            } else if (tag instanceof XsdRestriction) {
                type = new XsdRestriction(name, ((XsdRestriction) tag).getBase());
            } else if (tag instanceof XsdUnion) {
                type = new XsdUnion(name, ((XsdUnion) tag).getMemberTypes());
            }
        }
        return type;
    }

    private XsdList makeSimpleTypeList(Map<String, String> attributeMap, List<XsdTag> tags)
            throws XsdParserException {
        QName itemTypeName = parseQName(attributeMap.get("itemType"));

        XsdList list;
        if (itemTypeName != null) {
            list = new XsdList(null, new XsdTypeReferrer(itemTypeName));
        } else {
            XsdType type = null;
            for (XsdTag tag : tags) {
                if (tag == null) continue;
                if (tag instanceof XsdType) {
                    type = (XsdType) tag;
                }
            }
            list = new XsdList(null, new XsdTypeReferrer(type));
        }
        return list;
    }

    private XsdUnion makeSimpleTypeUnion(Map<String, String> attributeMap, List<XsdTag> tags)
            throws XsdParserException {
        List<QName> memberTypeNames = parseQNames(attributeMap.get("memberTypes"));
        List<XsdTypeReferrer> memberTypes = memberTypeNames.stream().map(
                XsdTypeReferrer::new).collect(Collectors.toList());

        for (XsdTag tag : tags) {
            if (tag == null) continue;
            if (tag instanceof XsdType) {
                memberTypes.add(new XsdTypeReferrer((XsdType) tag));
            }
        }
        return new XsdUnion(null, memberTypes);
    }

    private static List<XsdElement> makeSequence(Map<String, String> attributeMap,
            List<XsdTag> tags) throws XsdParserException {
        List<XsdElement> elements = new ArrayList<>();
        for (XsdTag tag : tags) {
            if (tag == null) continue;
            if (tag instanceof XsdElement) {
                elements.add((XsdElement) tag);
            }
        }
        return elements;
    }
}
