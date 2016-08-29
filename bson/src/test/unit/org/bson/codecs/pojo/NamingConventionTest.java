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

package org.bson.codecs.pojo;

import org.junit.Test;

import static org.bson.codecs.pojo.conventions.naming.NamingConventionsHelper.kebabCase;
import static org.bson.codecs.pojo.conventions.naming.NamingConventionsHelper.lowerCamelCase;
import static org.bson.codecs.pojo.conventions.naming.NamingConventionsHelper.snakeCase;
import static org.bson.codecs.pojo.conventions.naming.NamingConventionsHelper.upperCamelCase;
import static org.junit.Assert.assertEquals;

public class NamingConventionTest {

    @Test
    public void testKebabCase() throws Exception {
        assertEquals("kebab-case", kebabCase("kebabCase"));
        assertEquals("kebab-case", kebabCase("KebabCase"));
        assertEquals("kebab-case", kebabCase("Kebab_Case"));
        assertEquals("kebab-case", kebabCase("Kebab_case"));
        assertEquals("kebab-case", kebabCase("kebab_case"));
        assertEquals("kebab-case", kebabCase("kebab-case"));
        assertEquals("foo2", kebabCase("Foo2"));
    }

    @Test
    public void testLowerCamelCase() throws Exception {
        assertEquals("lowerCamelCase", lowerCamelCase("lowerCamelCase"));
        assertEquals("lowerCamelCase", lowerCamelCase("LowerCamelCase"));
        assertEquals("lowerCamelCase", lowerCamelCase("Lower_Camel_Case"));
        assertEquals("lowerCamelCase", lowerCamelCase("Lower_camel_case"));
        assertEquals("lowerCamelCase", lowerCamelCase("lower_Camel_case"));
        assertEquals("lowerCamelCase", lowerCamelCase("lower_camel_case"));
        assertEquals("foo2", lowerCamelCase("Foo2"));
    }

    @Test
    public void testSnakeCase() throws Exception {
        assertEquals("snake_case", snakeCase("snakeCase"));
        assertEquals("snake_case", snakeCase("SnakeCase"));
        assertEquals("snake_case", snakeCase("Snake_Case"));
        assertEquals("snake_case", snakeCase("Snake_case"));
        assertEquals("snake_case", snakeCase("snake_case"));
        assertEquals("snake_case", snakeCase("snake-case"));
        assertEquals("foo2", snakeCase("Foo2"));
        assertEquals("foo_", snakeCase("Foo_"));
        assertEquals("_foo", snakeCase("_Foo"));
        assertEquals("__foo", snakeCase("__Foo"));
    }

    @Test
    public void testUpperCamelCase() throws Exception {
        assertEquals("UpperCase", upperCamelCase("upperCase"));
        assertEquals("UpperCase", upperCamelCase("UpperCase"));
        assertEquals("UpperCase", upperCamelCase("Upper_Case"));
        assertEquals("UpperCase", upperCamelCase("Upper_case"));
        assertEquals("UpperCase", upperCamelCase("upper_case"));
        assertEquals("UpperCase", upperCamelCase("upper-case"));
        assertEquals("Foo2", upperCamelCase("foo2"));
    }
}
