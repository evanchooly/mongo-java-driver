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

/**
 * A StringChild with a name
 */
public class NamedStringChild extends StringChild {
    private String name;

    /**
     * For the serializer
     */
    public NamedStringChild() {
    }

    /**
     * Creates a StringChild with the given name
     * @param s the value
     * @param name the name
     */
    public NamedStringChild(final String s, final String name) {
        super(s);
        this.name = name;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name
     */
    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return String.format("NamedStringChild{name='%s'} %s", name, super.toString());
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NamedStringChild)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        NamedStringChild that = (NamedStringChild) o;

        return name != null ? name.equals(that.name) : that.name == null;

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }
}
