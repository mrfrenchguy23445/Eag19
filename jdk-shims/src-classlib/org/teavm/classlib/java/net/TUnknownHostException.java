/*
 *  Copyright 2017 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.teavm.classlib.java.net;

import java.io.IOException;

// teavm-classlib 0.12.1-EAGLER-R3 ships TUnknownServiceException, TMalformedURLException
// and TProtocolException but NOT TUnknownHostException.  ClasspathResourceMapper rewrites
// a requested java.net.UnknownHostException to org.teavm.classlib.java.net.TUnknownHostException
// (mapPackageHierarchy + stripPrefixFromPackageHierarchyClasses, see
// src/teavm/resources/META-INF/teavm.properties), finds nothing in any jar, and the class
// degrades to a hollow wasm-GC type: a root struct with java.lang.Object's exact shape.
// ServerSelectionList$OnlineServerEntry then emits `br_on_cast (ref null 8255) (ref null 36)`
// for its `catch (UnknownHostException)`, and V8 rejects the module because type 36 is
// neither a subtype of java.lang.Throwable nor of the label's Object result.
// Shipping the class restores the real hierarchy: UnknownHostException -> IOException.
public class TUnknownHostException extends IOException {
    public TUnknownHostException() {
        super();
    }

    public TUnknownHostException(String detailMessage) {
        super(detailMessage);
    }
}
