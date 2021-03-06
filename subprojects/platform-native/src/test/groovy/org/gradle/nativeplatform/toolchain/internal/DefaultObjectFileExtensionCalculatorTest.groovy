/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.nativeplatform.toolchain.internal

import spock.lang.Specification

class DefaultObjectFileExtensionCalculatorTest extends Specification {

    def "returns correct object file suffix for operating system" () {
        def os = Mock(AbstractPlatformToolProvider) {
            getPCHFileExtension() >> ".pch"
            getObjectFileExtension() >> ".o"
        }
        def spec = Mock(NativeCompileSpec) {
            isPrefixHeaderCompile() >> isPrefixHeaderCompile
        }
        ObjectFileExtensionCalculator calculator = new DefaultObjectFileExtensionCalculator(os)

        expect:
        calculator.transform(spec) == extension

        where:
        isPrefixHeaderCompile | extension
        true                | ".pch"
        false               | ".o"
    }
}
