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

package org.bson.codecs.pojo;

/**
 * Defines a method to determine if a FieldModel should be serialized or not for a given value.
 */
interface ShouldSerialize {
    /**
     * Determines if a model should be serialized
     *
     * @param model the configured model
     * @param value the value to consider
     * @return true if the model/value pair should be serialized
     */
    boolean evaluate(FieldModel model, Object value);
}
