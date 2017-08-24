/*
 *
 *  Copyright 2012 Netflix, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */
package com.netflix.simianarmy;

import java.util.Objects;

/**
 * The Class that holds the EC2 tags in name value pair.
 */
public class Tag {

    private String key;
    private String value;

    public Tag() {}

    public Tag(String key, String value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public String toString() {
        return "Tag{" +
                "key='" + key + '\'' +
                ", value='" + value + '\'' +
                '}';
    }

    @Override
    public int hashCode() {
        return (key+value).hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Tag tag = (Tag) obj;
        return Objects.equals(key, tag.getKey())
                && Objects.equals(value, tag.getValue());
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public void setName(String key) {
        this.key = key;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
