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

package org.gradle.nativeplatform.toolchain.internal.msvcpp;

import org.apache.commons.collections.CollectionUtils;
import org.gradle.api.Transformer;
import org.gradle.internal.operations.BuildOperationProcessor;
import org.gradle.nativeplatform.toolchain.internal.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class VisualCppNativeCompiler<T extends NativeCompileSpec> extends NativeCompiler<T> {

    VisualCppNativeCompiler(BuildOperationProcessor buildOperationProcessor, CommandLineToolInvocationWorker commandLineToolInvocationWorker, CommandLineToolContext invocationContext, ArgsTransformerFactory<T> argsTransformerFactory, Transformer<T, T> specTransformer, ObjectFileExtensionCalculator objectFileExtensionCalculator, boolean useCommandFile) {
        super(buildOperationProcessor, commandLineToolInvocationWorker, invocationContext, argsTransformerFactory, specTransformer, objectFileExtensionCalculator, useCommandFile);
    }

    @Override
    protected List<String> getOutputArgs(T spec, File outputFile) {
        // MSVC doesn't allow a space between Fo and the file name
        if (spec.isPrefixHeaderCompile()) {
            return Collections.singletonList("/Fp" + outputFile.getAbsolutePath());
        } else {
            return Collections.singletonList("/Fo" + outputFile.getAbsolutePath());
        }
    }

    @Override
    protected void addOptionsFileArgs(List<String> args, File tempDir) {
        OptionsFileArgsWriter writer = new VisualCppOptionsFileArgsWriter(tempDir);
        // modifies args in place
        writer.execute(args);
    }

    @Override
    protected List<String> getPCHArgs(T spec) {
        List<String> pchArgs = new ArrayList<String>();
        if (CollectionUtils.isNotEmpty(spec.getPreCompiledHeaders()) && spec.getPreCompiledHeaderObjectFile() != null) {
            String lastHeader = (String) CollectionUtils.get(spec.getPreCompiledHeaders(), spec.getPreCompiledHeaders().size() - 1);
            if (lastHeader.startsWith("<")) {
                lastHeader = lastHeader.substring(1, lastHeader.length()-1);
            }
            pchArgs.add("/Yu".concat(lastHeader));
            pchArgs.add("/Fp".concat(spec.getPreCompiledHeaderObjectFile().getAbsolutePath()));
        }
        return pchArgs;
    }
}
