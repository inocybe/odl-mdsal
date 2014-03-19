/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.sal.binding.generator.impl

import com.google.common.collect.ImmutableList
import java.util.ArrayList
import java.util.Collections
import java.util.HashMap
import java.util.List
import java.util.Map
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap
import org.opendaylight.yangtools.yang.binding.Augmentation
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.IdentifiableItem
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.Item
import org.opendaylight.yangtools.yang.binding.util.BindingReflections
import org.opendaylight.yangtools.yang.common.QName
import org.opendaylight.yangtools.yang.data.api.CompositeNode
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeIdentifier
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeIdentifierWithPredicates
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument
import org.opendaylight.yangtools.yang.data.api.Node
import org.opendaylight.yangtools.yang.data.impl.CompositeNodeTOImpl
import org.opendaylight.yangtools.yang.data.impl.SimpleNodeTOImpl
import org.opendaylight.yangtools.yang.data.impl.codec.CodecRegistry
import org.opendaylight.yangtools.yang.data.impl.codec.IdentifierCodec
import org.opendaylight.yangtools.yang.data.impl.codec.InstanceIdentifierCodec
import org.opendaylight.yangtools.yang.data.impl.codec.ValueWithQName
import org.slf4j.LoggerFactory

class InstanceIdentifierCodecImpl implements InstanceIdentifierCodec {

    private static val LOG = LoggerFactory.getLogger(InstanceIdentifierCodecImpl);
    val CodecRegistry codecRegistry;

    val Map<Class<?>, Map<List<QName>, Class<?>>> classToPreviousAugment = Collections.synchronizedMap(new WeakHashMap);

    public new(CodecRegistry registry) {
        codecRegistry = registry;
    }


    override deserialize(org.opendaylight.yangtools.yang.data.api.InstanceIdentifier input) {
        var Class<?> baType = null
        val biArgs = input.path
        val scannedPath = new ArrayList<QName>(biArgs.size);
        val baArgs = new ArrayList<InstanceIdentifier.PathArgument>(biArgs.size)
        for(biArg : biArgs) {
            scannedPath.add(biArg.nodeType);
            val baArg = deserializePathArgument(biArg,scannedPath)
            baType = baArg?.type
            val injectAugment = classToPreviousAugment.get(baType);
            if(injectAugment != null) {
                val augment = injectAugment.get(scannedPath) as Class<? extends DataObject>;
                if(augment != null) {
                    baArgs.add(new Item(augment));
                }
            }
            baArgs.add(baArg)
        }
        val ret = new InstanceIdentifier(baArgs,baType as Class<? extends DataObject>);
        LOG.debug("DOM Instance Identifier {} deserialized to {}",input,ret);
        return ret;
    }

    private def dispatch InstanceIdentifier.PathArgument deserializePathArgument(NodeIdentifier argument,List<QName> processedPath) {
        val Class cls = codecRegistry.getClassForPath(processedPath);
        return new Item(cls);
    }


    private def dispatch InstanceIdentifier.PathArgument deserializePathArgument(NodeIdentifierWithPredicates argument,List<QName> processedPath) {
        val Class type = codecRegistry.getClassForPath(processedPath);
        val IdentifierCodec codec = codecRegistry.getIdentifierCodecForIdentifiable(type);
        val value = codec.deserialize(argument.toCompositeNode())?.value;
        return CodecTypeUtils.newIdentifiableItem(type,value);
    }

    def CompositeNode toCompositeNode(NodeIdentifierWithPredicates predicates) {
        val keyValues = predicates.keyValues.entrySet;
        val values = new ArrayList<Node<?>>(keyValues.size)
        for(keyValue : keyValues) {
            values.add(new SimpleNodeTOImpl(keyValue.key,null,keyValue.value))
        }
        return new CompositeNodeTOImpl(predicates.nodeType,null,values);
    }

    override serialize(InstanceIdentifier<?> input) {
        var Class<?> previousAugmentation = null
        val pathArgs = input.path as List<InstanceIdentifier.PathArgument>
        var QName previousQName = null;
        val components = new ArrayList<PathArgument>(pathArgs.size);
        val qnamePath = new ArrayList<QName>(pathArgs.size);
        for(baArg : pathArgs) {

            if(!Augmentation.isAssignableFrom(baArg.type)) {
                val biArg = serializePathArgument(baArg,previousQName);
                previousQName = biArg.nodeType;
                components.add(biArg);
                qnamePath.add(biArg.nodeType);
                val immutableList = ImmutableList.copyOf(qnamePath);
                codecRegistry.putPathToClass(immutableList,baArg.type);
                if(previousAugmentation !== null) {
                    updateAugmentationInjection(baArg.type,immutableList,previousAugmentation)
                }

                previousAugmentation = null;
            } else {
                previousQName = codecRegistry.getQNameForAugmentation(baArg.type as Class<?>);
                previousAugmentation = baArg.type;
            }
        }
        val ret = new org.opendaylight.yangtools.yang.data.api.InstanceIdentifier(components);
        LOG.debug("Binding Instance Identifier {} serialized to DOM InstanceIdentifier {}",input,ret);
        return ret;
    }

    def updateAugmentationInjection(Class<? extends DataObject> class1, ImmutableList<QName> list, Class<?> augmentation) {
        if(classToPreviousAugment.get(class1) == null) {
            classToPreviousAugment.put(class1,new ConcurrentHashMap());
        }
        classToPreviousAugment.get(class1).put(list,augmentation);
    }

    private def dispatch PathArgument serializePathArgument(Item<?> argument, QName previousQname) {
        val type = argument.type;
        val qname = BindingReflections.findQName(type);
        if(previousQname == null || (BindingReflections.isAugmentationChild(argument.type))) {
            return new NodeIdentifier(qname);
        }
        return new NodeIdentifier(QName.create(previousQname,qname.localName));
    }

    @SuppressWarnings("rawtypes")
    private def dispatch PathArgument serializePathArgument(IdentifiableItem argument, QName previousQname) {
        val Map<QName,Object> predicates = new HashMap();
        val type = argument.type;
        val keyCodec = codecRegistry.getIdentifierCodecForIdentifiable(type);
        var QName qname = BindingReflections.findQName(type);
        if(previousQname != null && !(BindingReflections.isAugmentationChild(argument.type))) {
            qname = QName.create(previousQname,qname.localName);
        }
        val combinedInput =  new ValueWithQName(previousQname,argument.key)
        val compositeOutput = keyCodec.serialize(combinedInput as ValueWithQName);
        for(outputValue :compositeOutput.value) {
            predicates.put(outputValue.nodeType,outputValue.value);
        }
        if(previousQname == null) {
            return new NodeIdentifierWithPredicates(qname,predicates);
        }
        return new NodeIdentifierWithPredicates(qname,predicates);
    }
}
