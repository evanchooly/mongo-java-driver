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

package org.bson.codecs.configuration.mapper.entities;

import java.util.Map;

public class User extends Person {
    private Map<String, Address> addresses;

    public User() {
    }

    public User(final String firstName, final String lastName, final Address home,
                final Map<String, Address> addresses) {
        super(firstName, lastName, home);
        this.addresses = addresses;
    }

    public void add(final String name, final Address address) {
        addresses.put(name, address);
    }
    public Map<String, Address> getAddresses() {
        return addresses;
    }

    public User setAddresses(final Map<String, Address> addresses) {
        this.addresses = addresses;
        return this;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof User)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        User user = (User) o;

        return addresses != null ? addresses.equals(user.addresses) : user.addresses == null;

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (addresses != null ? addresses.hashCode() : 0);
        return result;
    }
}
