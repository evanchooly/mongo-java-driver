/*
 * Copyright (c) 2008-2016 MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.bson.codecs.pojo.entities;

import org.bson.codecs.pojo.conventions.annotations.Property;
import org.bson.types.ObjectId;

public class Complex {
    //CHECKSTYLE:OFF
    public static IntChild staticField = new IntChild(42);
    protected StringChild protectedField;
    protected transient BaseGenericType<Integer> transientField;
    //CHECKSTYLE:ON
    private ObjectId id;
    @Property(useDiscriminator = false)
    private IntChild intChild;
    private StringChild stringChild;
    private BaseGenericType<String> baseType;

    public Complex() {
    }

    public Complex(final IntChild intChild, final StringChild stringChild,
                   final BaseGenericType<String> baseType) {
        this.intChild = intChild;
        this.stringChild = stringChild;
        this.baseType = baseType;
    }

    public BaseGenericType<String> getBaseType() {
        return baseType;
    }

    public void setBaseType(final BaseGenericType<String> baseType) {
        this.baseType = baseType;
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(final ObjectId id) {
        this.id = id;
    }

    public IntChild getIntChild() {
        return intChild;
    }

    public void setIntChild(final IntChild intChild) {
        this.intChild = intChild;
    }

    public StringChild getProtectedField() {
        return protectedField;
    }

    public void setProtectedField(final StringChild protectedField) {
        this.protectedField = protectedField;
    }

    public StringChild getStringChild() {
        return stringChild;
    }

    public Complex setStringChild(final StringChild stringChild) {
        this.stringChild = stringChild;
        return this;
    }

    public BaseGenericType<Integer> getTransientField() {
        return transientField;
    }

    public void setTransientField(final BaseGenericType<Integer> transientField) {
        this.transientField = transientField;
    }

    @Override
    public int hashCode() {
        int result = intChild != null ? intChild.hashCode() : 0;
        result = 31 * result + (stringChild != null ? stringChild.hashCode() : 0);
        result = 31 * result + (baseType != null ? baseType.hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Complex)) {
            return false;
        }

        final Complex complex = (Complex) o;

        if (intChild != null ? !intChild.equals(complex.intChild) : complex.intChild != null) {
            return false;
        }
        if (stringChild != null ? !stringChild.equals(complex.stringChild) : complex.stringChild != null) {
            return false;
        }
        return baseType != null ? baseType.equals(complex.baseType) : complex.baseType == null;

    }

    @Override
    public String toString() {
        return String.format("Complex{baseType=%s, intChild=%s, stringChild=%s}", baseType, intChild, stringChild);
    }
}
