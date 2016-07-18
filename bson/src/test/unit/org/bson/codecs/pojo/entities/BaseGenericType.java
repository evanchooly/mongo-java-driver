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

import org.bson.types.ObjectId;

public class BaseGenericType<T> {
    private ObjectId id;
    private T t;

    public BaseGenericType() {
    }
    public BaseGenericType(final T t) {
        this.t = t;
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(final ObjectId id) {
        this.id = id;
    }

    public T getT() {
        return t;
    }

    public void setT(final T t) {
        this.t = t;
    }

    @Override
    public int hashCode() {
        return t != null ? t.hashCode() : 0;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BaseGenericType)) {
            return false;
        }

        final BaseGenericType<?> baseType = (BaseGenericType<?>) o;

        return t != null ? t.equals(baseType.t) : baseType.t == null;

    }

    @Override
    public String toString() {
        return String.format("%s{t=%s}", getClass().getSimpleName(), t);
    }

}
