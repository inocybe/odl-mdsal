/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.binding.generator.util.generated.type.builder;

import java.util.Objects;
import org.opendaylight.yangtools.sal.binding.model.api.Constant;
import org.opendaylight.yangtools.sal.binding.model.api.Type;

final class ConstantImpl implements Constant {

    private final Type definingType;
    private final Type type;
    private final String name;
    private final Object value;

    public ConstantImpl(final Type definingType, final Type type, final String name, final Object value) {
        super();
        this.definingType = definingType;
        this.type = type;
        this.name = name;
        this.value = value;
    }

    @Override
    public Type getDefiningType() {
        return definingType;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public String toFormattedString() {
        StringBuilder builder = new StringBuilder();
        builder.append(type);
        builder.append(" ");
        builder.append(name);
        builder.append(" ");
        builder.append(value);
        return builder.toString();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Objects.hashCode(name);
        result = prime * result + Objects.hashCode(type);
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ConstantImpl other = (ConstantImpl) obj;
        return Objects.equals(name, other.name) && Objects.equals(type, other.type) && Objects.equals(value, other.value);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Constant [type=");
        builder.append(type);
        builder.append(", name=");
        builder.append(name);
        builder.append(", value=");
        builder.append(value);
        if (definingType != null) {
            builder.append(", definingType=");
            builder.append(definingType.getPackageName());
            builder.append(".");
            builder.append(definingType.getName());
        } else {
            builder.append(", definingType= null");
        }
        builder.append("]");
        return builder.toString();
    }
}
