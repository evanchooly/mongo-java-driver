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

import static java.lang.Character.toUpperCase;

/**
 * Provides a naming convention implementation for mapping Java names to MongoDB names.
 *
 * @since 3.4
 */
public final class NamingConventionsHelper {
    private NamingConventionsHelper() {
    }

    /**
     * Converts the name to lower camel case.
     *
     * @param name the name to update
     * @return the new form of the name
     */
    public static String lowerCamelCase(final String name) {
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            if (builder.length() == 0) {
                builder.append(Character.toLowerCase(name.charAt(0)));
            } else {
                i = camelCase(name, builder, i);
            }
        }
        return builder.toString();
    }

    /**
     * Converts the name to snake case.
     *
     * @param name the name to update
     * @return the new form of the name
     */
    public static String snakeCase(final String name) {
        return convert(name, '_');
    }

    /**
     * Converts the name to kebab case.
     *
     * @param name the name to update
     * @return the new form of the name
     */
    public static String kebabCase(final String name) {
        return convert(name, '-');
    }

    private static String convert(final String name, final char separator) {
        final String normalized = name.replaceAll("[_-]", separator + "");
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < normalized.length(); i++) {
            final char c = normalized.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i != 0) {
                    appendOnlyOnce(builder, separator);
                }
            }
            builder.append(Character.toLowerCase(c));
        }
        return builder.toString();
    }

    private static StringBuilder appendOnlyOnce(final StringBuilder builder, final char value) {
        return builder.length() == 0 || builder.charAt(builder.length() - 1) != value
               ? builder.append(value)
               : builder;
    }

    /**
     * Converts the name to upper camel case.
     *
     * @param name the name to update
     * @return the new form of the name
     */
    public static String upperCamelCase(final String name) {
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            final char c = name.charAt(0);

            if (builder.length() == 0) {
                builder.append(toUpperCase(c));
            } else {
                i = camelCase(name, builder, i);
            }
        }
        return builder.toString();
    }

    private static int camelCase(final String name, final StringBuilder builder, final int index) {
        int position = index;
        final char c = name.charAt(position);
        if (!Character.isLetter(c) && (index < name.length() - 1)) {
            builder.append(toUpperCase(name.charAt(++position)));
        } else {
            builder.append(c);
        }

        return position;
    }
}
