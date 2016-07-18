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

import com.fasterxml.classmate.AnnotationConfiguration.StdConfiguration;
import com.fasterxml.classmate.AnnotationInclusion;
import com.fasterxml.classmate.MemberResolver;
import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.ResolvedTypeWithMembers;
import com.fasterxml.classmate.TypeResolver;
import com.fasterxml.classmate.members.ResolvedField;
import org.bson.codecs.pojo.entities.BaseGenericType;
import org.bson.codecs.pojo.entities.ContainerTypes;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;

@SuppressWarnings("rawtypes")
public class FieldModelTest {
    @Test
    public void testExtract() {
        TypeResolver resolver = new TypeResolver();

        MemberResolver memberResolver = new MemberResolver(resolver);
        ResolvedType resolved = resolver.resolve(ContainerTypes.class);
        ResolvedTypeWithMembers type =
            memberResolver.resolve(resolved, new StdConfiguration(AnnotationInclusion.INCLUDE_AND_INHERIT_IF_INHERITED), null);

        ResolvedField[] fields = type.getMemberFields();
        for (ResolvedField field : fields) {
            if("tripleList".equals(field.getName())) {
                compare(FieldModel.extract(field.getType()),
                        Arrays.<Class>asList(ArrayList.class, ArrayList.class, ArrayList.class, BaseGenericType.class));
            } else if ("tripleMap".equals(field.getName())) {
                compare(FieldModel.extract(field.getType()), Arrays.<Class>asList(HashMap.class, HashMap.class, HashMap.class, BaseGenericType.class));
            } else if ("mixed".equals(field.getName())) {
                compare(FieldModel.extract(field.getType()),
                        Arrays.<Class>asList(HashMap.class, ArrayList.class, HashSet.class, BaseGenericType.class));
            }
        }

    }

    protected void compare(final List<Class> list, final List<Class> expected) {
        assertEquals(expected.size(), list.size());
        Iterator<Class> iterator = list.iterator();
        Iterator<Class> expectedItertator = expected.iterator();
        while(iterator.hasNext()) {
            assertEquals(expectedItertator.next(), iterator.next());
        }
    }
}