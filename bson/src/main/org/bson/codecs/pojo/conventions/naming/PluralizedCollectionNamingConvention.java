/*
 * Copyright 2008-2016 MongoDB, Inc.
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

package org.bson.codecs.pojo.conventions.naming;

import org.bson.codecs.pojo.ClassModel.Builder;

/**
 * A naming convention using a pluralized lower camel case naming convention.
 *
 * @since 3.4
 */
public class PluralizedCollectionNamingConvention extends CollectionNamingConvention {
    @Override
    public void nameCollection(final Builder model) {
        String collectionName = NamingConventionsHelper.lowerCamelCase(model.getTypeName());
        collectionName += collectionName.endsWith("s") ? "es" : "s";
        model.collection(collectionName);
    }
}
