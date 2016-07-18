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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A test entity containing various forms of nested container types.
 */
public class ContainerTypes {
    private ObjectId id;

    private Collection<? extends BaseGenericType<?>> collection;
    private Collection<Collection<? extends BaseGenericType<?>>> doubleCollection;
    private Collection<Collection<Collection<? extends BaseGenericType<?>>>> tripleCollection;

    private List<? extends BaseGenericType<?>> list;
    private List<List<? extends BaseGenericType<?>>> doubleList;
    private List<List<List<? extends BaseGenericType<?>>>> tripleList;

    private Map<String, BaseGenericType<?>> map;
    private Map<String, Map<String, BaseGenericType<?>>> doubleMap;
    private Map<String, Map<String, Map<String, BaseGenericType<?>>>> tripleMap;

    private Set<? extends BaseGenericType<?>> set;
    private Set<Set<? extends BaseGenericType<?>>> doubleSet;
    private Set<Set<Set<? extends BaseGenericType<?>>>> tripleSet;

    private Map<String, List<Set<? extends BaseGenericType<?>>>> mixed;

    public Collection<? extends BaseGenericType<?>> getCollection() {
        return collection;
    }

    public void setCollection(final Collection<? extends BaseGenericType<?>> collection) {
        this.collection = collection;
    }

    public Collection<Collection<? extends BaseGenericType<?>>> getDoubleCollection() {
        return doubleCollection;
    }

    public void setDoubleCollection(
        final Collection<Collection<? extends BaseGenericType<?>>> doubleCollection) {
        this.doubleCollection = doubleCollection;
    }

    /**
     * @return the double list
     */
    public List<List<? extends BaseGenericType<?>>> getDoubleList() {
        return doubleList;
    }

    /**
     * @param doubleList the double list
     */
    public void setDoubleList(final List<List<? extends BaseGenericType<?>>> doubleList) {
        this.doubleList = doubleList;
    }

    /**
     * @return the double map
     */
    public Map<String, Map<String, BaseGenericType<?>>> getDoubleMap() {
        return doubleMap;
    }

    /**
     * @param doubleMap the double map
     */
    public void setDoubleMap(final Map<String, Map<String, BaseGenericType<?>>> doubleMap) {
        this.doubleMap = doubleMap;
    }

    /**
     * @return the double set
     */
    public Set<Set<? extends BaseGenericType<?>>> getDoubleSet() {
        return doubleSet;
    }

    /**
     * @param doubleSet the double set
     */
    public void setDoubleSet(final Set<Set<? extends BaseGenericType<?>>> doubleSet) {
        this.doubleSet = doubleSet;
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(final ObjectId id) {
        this.id = id;
    }

    /**
     * @return the list
     */
    public List<? extends BaseGenericType<?>> getList() {
        return list;
    }

    /**
     * @param list the list
     */
    public void setList(final List<? extends BaseGenericType<?>> list) {
        this.list = list;
    }

    /**
     * @return the map
     */
    public Map<String, BaseGenericType<?>> getMap() {
        return map;
    }

    /**
     * @param map the map
     */
    public void setMap(final Map<String, BaseGenericType<?>> map) {
        this.map = map;
    }

    /**
     * @return the mixed container
     */
    public Map<String, List<Set<? extends BaseGenericType<?>>>> getMixed() {
        return mixed;
    }

    /**
     * @param mixed the mixed container
     */
    public void setMixed(
        final Map<String, List<Set<? extends BaseGenericType<?>>>> mixed) {
        this.mixed = mixed;
    }

    /**
     * @return the set
     */
    public Set<? extends BaseGenericType<?>> getSet() {
        return set;
    }

    /**
     * @param set the set
     */
    public void setSet(final Set<? extends BaseGenericType<?>> set) {
        this.set = set;
    }

    public Collection<Collection<Collection<? extends BaseGenericType<?>>>> getTripleCollection() {
        return tripleCollection;
    }

    public void setTripleCollection(
        final Collection<Collection<Collection<? extends BaseGenericType<?>>>> tripleCollection) {
        this.tripleCollection = tripleCollection;
    }

    /**
     * @return the triple list
     */
    public List<List<List<? extends BaseGenericType<?>>>> getTripleList() {
        return tripleList;
    }

    /**
     * @param tripleList the triple list
     */
    public void setTripleList(final List<List<List<? extends BaseGenericType<?>>>> tripleList) {
        this.tripleList = tripleList;
    }

    /**
     * @return the triple map
     */
    public Map<String, Map<String, Map<String, BaseGenericType<?>>>> getTripleMap() {
        return tripleMap;
    }

    /**
     * @param tripleMap the triple map
     */
    public void setTripleMap(final Map<String, Map<String, Map<String, BaseGenericType<?>>>> tripleMap) {
        this.tripleMap = tripleMap;
    }

    /**
     * @return the triple set
     */
    public Set<Set<Set<? extends BaseGenericType<?>>>> getTripleSet() {
        return tripleSet;
    }

    /**
     * @param tripleSet the triple set
     */
    public void setTripleSet(final Set<Set<Set<? extends BaseGenericType<?>>>> tripleSet) {
        this.tripleSet = tripleSet;
    }

    @Override
    public int hashCode() {
        int result = getId() != null ? getId().hashCode() : 0;
        result = 31 * result + (getCollection() != null ? getCollection().hashCode() : 0);
        result = 31 * result + (getDoubleCollection() != null ? getDoubleCollection().hashCode() : 0);
        result = 31 * result + (getTripleCollection() != null ? getTripleCollection().hashCode() : 0);
        result = 31 * result + (getList() != null ? getList().hashCode() : 0);
        result = 31 * result + (getDoubleList() != null ? getDoubleList().hashCode() : 0);
        result = 31 * result + (getTripleList() != null ? getTripleList().hashCode() : 0);
        result = 31 * result + (getMap() != null ? getMap().hashCode() : 0);
        result = 31 * result + (getDoubleMap() != null ? getDoubleMap().hashCode() : 0);
        result = 31 * result + (getTripleMap() != null ? getTripleMap().hashCode() : 0);
        result = 31 * result + (getSet() != null ? getSet().hashCode() : 0);
        result = 31 * result + (getDoubleSet() != null ? getDoubleSet().hashCode() : 0);
        result = 31 * result + (getTripleSet() != null ? getTripleSet().hashCode() : 0);
        result = 31 * result + (getMixed() != null ? getMixed().hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ContainerTypes)) {
            return false;
        }

        ContainerTypes that = (ContainerTypes) o;

        if (getId() != null ? !getId().equals(that.getId()) : that.getId() != null) {
            return false;
        }
        if (getCollection() != null ? !getCollection().equals(that.getCollection()) : that.getCollection() != null) {
            return false;
        }
        if (getDoubleCollection() != null ? !getDoubleCollection().equals(that.getDoubleCollection())
                                          : that.getDoubleCollection() != null) {
            return false;
        }
        if (getTripleCollection() != null ? !getTripleCollection().equals(that.getTripleCollection())
                                          : that.getTripleCollection() != null) {
            return false;
        }
        if (getList() != null ? !getList().equals(that.getList()) : that.getList() != null) {
            return false;
        }
        if (getDoubleList() != null ? !getDoubleList().equals(that.getDoubleList()) : that.getDoubleList() != null) {
            return false;
        }
        if (getTripleList() != null ? !getTripleList().equals(that.getTripleList()) : that.getTripleList() != null) {
            return false;
        }
        if (getMap() != null ? !getMap().equals(that.getMap()) : that.getMap() != null) {
            return false;
        }
        if (getDoubleMap() != null ? !getDoubleMap().equals(that.getDoubleMap()) : that.getDoubleMap() != null) {
            return false;
        }
        if (getTripleMap() != null ? !getTripleMap().equals(that.getTripleMap()) : that.getTripleMap() != null) {
            return false;
        }
        if (getSet() != null ? !getSet().equals(that.getSet()) : that.getSet() != null) {
            return false;
        }
        if (getDoubleSet() != null ? !getDoubleSet().equals(that.getDoubleSet()) : that.getDoubleSet() != null) {
            return false;
        }
        if (getTripleSet() != null ? !getTripleSet().equals(that.getTripleSet()) : that.getTripleSet() != null) {
            return false;
        }
        return getMixed() != null ? getMixed().equals(that.getMixed()) : that.getMixed() == null;

    }

    protected boolean compare(final List<?> l1, final List<?> l2) {
        for (int i = 0; i < l1.size(); i++) {
            return compare(l1.get(i), l2.get(i));
        }
        return true;
    }

    protected boolean compare(final Object o1, final Object o2) {
        if (o1 instanceof List) {
            return compare((List<?>) o1, (List<?>) o2);
        }
        return o1.equals(o2);
    }
}
