/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.sal.java.api.generator;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import org.opendaylight.yangtools.sal.java.api.generator.EnumTemplate;
import org.opendaylight.yangtools.sal.binding.model.api.CodeGenerator;
import org.opendaylight.yangtools.sal.binding.model.api.Enumeration;
import org.opendaylight.yangtools.sal.binding.model.api.Type;

public class EnumGenerator implements CodeGenerator {

    @Override
    public Writer generate(Type type) throws IOException {
        final Writer writer = new StringWriter();
        if (type instanceof Enumeration) {
            final Enumeration enums = (Enumeration) type;
            final EnumTemplate enumTemplate = new EnumTemplate(enums);
            writer.write(enumTemplate.generate().toString());
        }
        return writer;
    }

}