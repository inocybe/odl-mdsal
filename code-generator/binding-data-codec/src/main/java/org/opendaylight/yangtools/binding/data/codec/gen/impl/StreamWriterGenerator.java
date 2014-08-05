/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.binding.data.codec.gen.impl;

import java.util.HashMap;
import java.util.Map;

import org.opendaylight.yangtools.binding.data.codec.util.AugmentableDispatchSerializer;
import org.opendaylight.yangtools.binding.data.codec.util.ChoiceDispatchSerializer;
import org.opendaylight.yangtools.sal.binding.generator.util.JavassistUtils;
import org.opendaylight.yangtools.sal.binding.model.api.GeneratedType;
import org.opendaylight.yangtools.sal.binding.model.api.MethodSignature;
import org.opendaylight.yangtools.sal.binding.model.api.ParameterizedType;
import org.opendaylight.yangtools.sal.binding.model.api.Type;
import org.opendaylight.yangtools.yang.binding.BindingMapping;
import org.opendaylight.yangtools.yang.binding.BindingStreamEventWriter;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.DataObjectSerializerImplementation;
import org.opendaylight.yangtools.yang.binding.DataObjectSerializerRegistry;
import org.opendaylight.yangtools.yang.model.api.AnyXmlSchemaNode;
import org.opendaylight.yangtools.yang.model.api.AugmentationSchema;
import org.opendaylight.yangtools.yang.model.api.ChoiceCaseNode;
import org.opendaylight.yangtools.yang.model.api.ChoiceNode;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.BooleanTypeDefinition;

/**
 *
 * Concrete implementation of {@link AbstractStreamWriterGenerator}
 * which in runtime generates classes implementing {@link DataObjectSerializerImplementation}
 * interface and are used to serialize Binding {@link DataObject}.
 *
 * Actual implementation of codecs is done via static methods, which allows
 * for static wiring of codecs. Choice codec and Augmentable codecs
 * are static properties of parent codec and stateless implementations
 * are used ( {@link ChoiceDispatchSerializer}, {@link AugmentableDispatchSerializer},
 * which uses registry to dispatch to concrete item codec.
 *
 */
public class StreamWriterGenerator extends AbstractStreamWriterGenerator {

    public StreamWriterGenerator(final JavassistUtils utils) {
        super(utils);
    }

    private abstract class DataNodeContainerSerializerSource extends DataObjectSerializerSource {

        protected static final String INPUT = "_input";
        protected static final String CHOICE_SERIALIZER = "CHOICE_SERIALIZER";
        protected static final String AUGMENTABLE_SERIALIZER = "AUGMENTABLE_SERIALIZER";

        GeneratedType dtoType;
        DataNodeContainer schemaNode;

        public DataNodeContainerSerializerSource(final GeneratedType type, final DataNodeContainer node) {
            this.dtoType = type;
            this.schemaNode = node;
            customize();
        }

        protected void customize() {
            // Intentionally NOOP
        }

        public abstract CharSequence emitStartEvent();

        @Override
        protected CharSequence getSerializerBody() {
            StringBuilder b = new StringBuilder();
            b.append("{\n");
            b.append(statement(assign(DataObjectSerializerRegistry.class.getName(), REGISTRY, "$1")));
            b.append(statement(assign(dtoType.getFullyQualifiedName(), INPUT,
                    cast(dtoType.getFullyQualifiedName(), "$2"))));
            b.append(statement(assign(BindingStreamEventWriter.class.getName(), STREAM, cast(BindingStreamEventWriter.class.getName(), "$3"))));
            b.append(statement(emitStartEvent()));

            emitBody(b);
            emitAfterBody(b);
            b.append(statement(endNode()));
            b.append(statement("return null"));
            b.append("}");
            return b;
        }

        /**
         * Allows for customization of emitting code, which is processed after
         * normal DataNodeContainer body. Ideal for augmentations or others.
         */
        protected void emitAfterBody(final StringBuilder b) {
            //
        }

        private void emitBody(final StringBuilder b) {
            Map<String, Type> getterToType = collectAllProperties(dtoType, new HashMap<String, Type>());
            for (DataSchemaNode schemaChild : schemaNode.getChildNodes()) {
                if (!schemaChild.isAugmenting()) {
                    String getter = getGetterName(schemaChild);
                    Type childType = getterToType.get(getter);
                    emitChild(b, getter, childType, schemaChild);
                }
            }
        }

        private void emitChild(final StringBuilder b, final String getterName, final Type childType,
                final DataSchemaNode schemaChild) {
            b.append(statement(assign(childType, getterName, cast(childType, invoke(INPUT, getterName)))));

            b.append("if (").append(getterName).append(" != null) {\n");
            emitChildInner(b, getterName, childType, schemaChild);
            b.append("}\n");
        }

        private void emitChildInner(final StringBuilder b, final String getterName, final Type childType,
                final DataSchemaNode child) {
            if (child instanceof LeafSchemaNode) {
                b.append(statement(leafNode(child.getQName().getLocalName(), getterName)));
            } else if (child instanceof AnyXmlSchemaNode) {
                b.append(statement(anyxmlNode(child.getQName().getLocalName(), getterName)));
            } else if (child instanceof LeafListSchemaNode) {
                b.append(statement(startLeafSet(child.getQName().getLocalName(),invoke(getterName, "size"))));
                Type valueType = ((ParameterizedType) childType).getActualTypeArguments()[0];
                b.append(forEach(getterName, valueType, statement(leafSetEntryNode(CURRENT))));
                b.append(statement(endNode()));
            } else if (child instanceof ListSchemaNode) {
                Type valueType = ((ParameterizedType) childType).getActualTypeArguments()[0];
                ListSchemaNode casted = (ListSchemaNode) child;
                emitList(b, getterName, valueType, casted);
            } else if (child instanceof ContainerSchemaNode) {
                b.append(statement(staticInvokeEmitter(childType, getterName)));
            } else if (child instanceof ChoiceNode) {
                String propertyName = "CHOICE_" + childType.getName();
                staticConstant(propertyName, DataObjectSerializerImplementation.class, ChoiceDispatchSerializer.from(loadClass(childType)));
                b.append(statement(invoke(propertyName, SERIALIZE_METHOD_NAME, REGISTRY, cast(DataObject.class.getName(),getterName), STREAM)));
            }
        }

        private void emitList(final StringBuilder b, final String getterName, final Type valueType,
                final ListSchemaNode child) {
            final CharSequence startEvent;

            b.append(statement(assign("int", "_count", invoke(getterName, "size"))));
            if (child.getKeyDefinition().isEmpty()) {
                startEvent = startUnkeyedList(classReference(valueType), "_count");
            } else if (child.isUserOrdered()) {
                startEvent = startOrderedMapNode(classReference(valueType), "_count");
            } else {
                startEvent = startMapNode(classReference(valueType), "_count");
            }
            b.append(statement(startEvent));
            b.append(forEach(getterName, valueType, statement(staticInvokeEmitter(valueType, CURRENT))));
            b.append(statement(endNode()));
        }
    }

    protected abstract class AugmentableDataNodeContainerEmmiterSource extends DataNodeContainerSerializerSource {

        public AugmentableDataNodeContainerEmmiterSource(final GeneratedType type, final DataNodeContainer node) {
            super(type, node);
            staticConstant(AUGMENTABLE_SERIALIZER, DataObjectSerializerImplementation.class,AUGMENTABLE);
        }

        @Override
        protected void emitAfterBody(final StringBuilder b) {
            b.append(statement(invoke(AUGMENTABLE_SERIALIZER, "serialize",REGISTRY, INPUT, STREAM)));
        }
    }

    private static Map<String, Type> collectAllProperties(final GeneratedType type, final Map<String, Type> hashMap) {
        for (MethodSignature definition : type.getMethodDefinitions()) {
            hashMap.put(definition.getName(), definition.getReturnType());
        }
        for (Type parent : type.getImplements()) {
            if (parent instanceof GeneratedType) {
                collectAllProperties((GeneratedType) parent, hashMap);
            }
        }
        return hashMap;
    }



    private CharSequence getChildSizeFromSchema(final DataNodeContainer node) {
        return Integer.toString(node.getChildNodes().size());
    }

    @Override
    DataObjectSerializerSource generateContainerSerializer(final GeneratedType type, final ContainerSchemaNode node) {

        return new DataNodeContainerSerializerSource(type, node) {
            @Override
            public CharSequence emitStartEvent() {
                return startContainerNode(classReference(type), getChildSizeFromSchema(node));
            }
        };
    }

    protected static final String getGetterName(final DataSchemaNode node) {
        final TypeDefinition<?> type ;
        if (node instanceof LeafSchemaNode) {
            type = ((LeafSchemaNode) node).getType();
        } else if(node instanceof LeafListSchemaNode) {
            type = ((LeafListSchemaNode) node).getType();
        } else {
            type = null;
        }
        String prefix = "get";
        if(type != null) {
            TypeDefinition<?> rootType = type;
            while (rootType.getBaseType() != null) {
                rootType = rootType.getBaseType();
            }
            if(rootType instanceof BooleanTypeDefinition) {
                prefix = "is";
            }
        }

        return prefix + BindingMapping.getClassName(node.getQName().getLocalName());
    }

    @Override
    DataObjectSerializerSource generateCaseSerializer(final GeneratedType type, final ChoiceCaseNode node) {
        return new AugmentableDataNodeContainerEmmiterSource(type, node) {
            @Override
            public CharSequence emitStartEvent() {
                return startCaseNode(classReference(type),getChildSizeFromSchema(node));
            }
        };
    }

    @Override
    DataObjectSerializerSource generateUnkeyedListEntrySerializer(final GeneratedType type, final ListSchemaNode node) {
        return new AugmentableDataNodeContainerEmmiterSource(type, node) {

            @Override
            public CharSequence emitStartEvent() {
                return startUnkeyedListItem(getChildSizeFromSchema(schemaNode));
            }
        };
    }

    @Override
    DataObjectSerializerSource generateSerializer(final GeneratedType type, final AugmentationSchema schema) {
        return new DataNodeContainerSerializerSource(type,schema) {

            @Override
            public CharSequence emitStartEvent() {
                return startAugmentationNode(classReference(type));
            }
        };
    }

    @Override
    DataObjectSerializerSource generateMapEntrySerializer(final GeneratedType type, final ListSchemaNode node) {
        return new AugmentableDataNodeContainerEmmiterSource(type, node) {

            @Override
            public CharSequence emitStartEvent() {
                return startMapEntryNode(invoke(INPUT, "getKey"), getChildSizeFromSchema(node));
            }
        };
    }

}
