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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Post {
    private ObjectId id;
    private String title;
    private Date posted;
    private String body;
    private List<Comment> comments = new ArrayList<Comment>();
    private List<Comment> bare = new ArrayList<Comment>();

    public List<Comment> getBare() {
        return bare;
    }

    public void setBare(final List<Comment> bare) {
        this.bare = bare;
    }

    public String getBody() {
        return body;
    }

    public void setBody(final String body) {
        this.body = body;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(final List<Comment> comments) {
        this.comments = comments;
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(final ObjectId id) {
        this.id = id;
    }

    public Date getPosted() {
        return posted;
    }

    public void setPosted(final Date posted) {
        this.posted = posted;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    @Override
    public int hashCode() {
        int result = title != null ? title.hashCode() : 0;
        result = 31 * result + (posted != null ? posted.hashCode() : 0);
        result = 31 * result + (body != null ? body.hashCode() : 0);
        result = 31 * result + (comments != null ? comments.hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Post)) {
            return false;
        }

        final Post post = (Post) o;

        if (title != null ? !title.equals(post.title) : post.title != null) {
            return false;
        }
        if (posted != null ? !posted.equals(post.posted) : post.posted != null) {
            return false;
        }
        if (body != null ? !body.equals(post.body) : post.body != null) {
            return false;
        }
        return comments != null ? comments.equals(post.comments) : post.comments == null;

    }
}
