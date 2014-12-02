/*
 * Copyright 2014 the original author or authors.
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

package org.gradle.integtests.language
import com.sun.xml.internal.ws.util.StringUtils
import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.integtests.fixtures.jvm.TestJvmComponent
import org.gradle.test.fixtures.archive.JarTestFixture

abstract class AbstractJvmLanguageIntegrationTest extends AbstractIntegrationSpec{

    abstract TestJvmComponent getApp()

    def setup() {
        buildFile << """
        plugins {
            id 'jvm-component'
            id '${app.languageName}-lang'
        }
        repositories{
            mavenCentral()
        }
    """
    }

    def "can build binary with sources in conventional location"() {
        when:
        app.writeSources(file("src/myLib"))
        app.writeResources(file("src/myLib/resources"))
        def expectedOutputs = app.expectedOutputs*.fullPath as String[]

        and:
        buildFile << """
    model {
        components {
            myLib(JvmLibrarySpec)
        }
    }

"""
        and:
        succeeds "assemble"

        then:
        executedAndNotSkipped ":processMyLibJarMyLibResources", ":compileMyLibJarMyLib${StringUtils.capitalize(app.languageName)}", ":createMyLibJar", ":myLibJar"

        and:
        file("build/classes/myLibJar").assertHasDescendants(expectedOutputs)
        jarFile("build/jars/myLibJar/myLib.jar").hasDescendants(expectedOutputs)
    }

    def "generated binary includes compiled classes from all language source sets"() {
        setup:
        def extraSourceSetName = "extra${app.languageName}"

        when:
        def source1 = app.sources[0]
        def source2 = app.sources[1]

        source1.writeToDir(file("src/myLib/${app.languageName}"))
        source2.writeToDir(file("src/myLib/$extraSourceSetName"))

        buildFile << """
    model {
        components {
            myLib(JvmLibrarySpec) {
                sources {
                    $extraSourceSetName(${app.sourceSetTypeName})
                }
            }
        }
    }
"""
        and:
        succeeds "assemble"

        then:
        executedAndNotSkipped ":compileMyLibJarMyLib${StringUtils.capitalize(app.languageName)}", ":compileMyLibJarMyLib${StringUtils.capitalize(extraSourceSetName)}", ":createMyLibJar", ":myLibJar"

        and:
        file("build/classes/myLibJar").assertHasDescendants(source1.classFile.fullPath, source2.classFile.fullPath)

        and:
        def jar = jarFile("build/jars/myLibJar/myLib.jar")
        jar.hasDescendants(source1.classFile.fullPath, source2.classFile.fullPath)
    }

    def "can configure source locations for language and resource source sets"() {
        setup:
        def customSourceSetName = "my${app.languageName}"
        app.writeSources(file("src/myLib"), customSourceSetName)
        app.writeResources(file("src/myLib/myResources"))

        // Conventional locations are ignore with explicit configuration
        file("src/myLib/${app.languageName}/Ignored.${app.languageName}") << "IGNORE ME"
        file("src/myLib/resources/Ignored.txt") << "IGNORE ME"

        buildFile << """
    model {
        components {
            myLib(JvmLibrarySpec) {
                sources {
                    ${app.languageName} {
                        source.srcDir "src/myLib/$customSourceSetName"
                    }
                    resources {
                        source.srcDir "src/myLib/myResources"
                    }
                }
            }
        }
    }
"""
        when:
        succeeds "assemble"

        then:
        file("build/classes/myLibJar").assertHasDescendants(app.expectedOutputs*.fullPath as String[])
        jarFile("build/jars/myLibJar/myLib.jar").hasDescendants(app.expectedOutputs*.fullPath as String[])
    }
//
    def "can combine resources and sources in a single source directory"() {
        when:
        app.writeSources(file("src/myLib"))
        app.writeResources(file("src/myLib"))

        String[] expectedOutputs = [app.sources[0].classFile.fullPath, app.sources[1].classFile.fullPath, app.resources[0].fullPath, app.resources[1].fullPath]

        buildFile << """
    model {
        components {
            myLib(JvmLibrarySpec) {
                sources {
                    ${app.languageName}.source {
                        srcDir "src/myLib"
                        exclude "**/*.txt"
                    }
                    resources.source {
                        srcDir "src/myLib"
                        exclude "**/*.${app.languageName}"
                    }
                }
            }
        }
    }
"""
        and:
        succeeds "assemble"

        then:
        file("build/classes/myLibJar").assertHasDescendants(expectedOutputs)
        jarFile("build/jars/myLibJar/myLib.jar").hasDescendants(expectedOutputs)
    }

    def "can configure output directories for classes and resources"() {
        when:
        app.writeSources(file("src/myLib"))
        app.writeResources(file("src/myLib/resources"))
        def expectedOutputs = app.expectedOutputs*.fullPath as String[]

        and:
        buildFile << """
    model {
        components {
            myLib(JvmLibrarySpec)
        }
        jvm {
            allBinaries {
                classesDir = file("\${project.buildDir}/custom-classes")
                resourcesDir = file("\${project.buildDir}/custom-resources")
            }
        }
    }
"""
        and:
        succeeds "assemble"

        then:
        executedAndNotSkipped ":processMyLibJarMyLibResources", ":compileMyLibJarMyLib${StringUtils.capitalize(app.languageName)}", ":createMyLibJar", ":myLibJar"

        and:
        file("build/custom-classes").assertHasDescendants(app.sources*.classFile.fullPath as String[])
        file("build/custom-resources").assertHasDescendants(app.resources*.fullPath as String[])

        and:
        jarFile("build/jars/myLibJar/myLib.jar").hasDescendants(expectedOutputs)
    }

    protected JarTestFixture jarFile(String s) {
        new JarTestFixture(file(s))
    }

}