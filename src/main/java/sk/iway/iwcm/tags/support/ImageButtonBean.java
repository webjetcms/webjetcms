/*
 * $Id$
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package sk.iway.iwcm.tags.support;

import java.io.Serializable;

public class ImageButtonBean implements Serializable {

    private String x;
    private String y;

    public ImageButtonBean() {
        // do nothing
    }

    public ImageButtonBean(String x, String y) {
        this.x = x;
        this.y = y;
    }

    public String getX() {
        return (this.x);
    }

    public void setX(String x) {
        this.x = x;
    }

    public String getY() {
        return (this.y);
    }

    public void setY(String y) {
        this.y = y;
    }

    public boolean isSelected() {
        return ((x != null) || (y != null));
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("ImageButtonBean[");

        sb.append(this.x);
        sb.append(", ");
        sb.append(this.y);
        sb.append("]");

        return (sb.toString());
    }
}