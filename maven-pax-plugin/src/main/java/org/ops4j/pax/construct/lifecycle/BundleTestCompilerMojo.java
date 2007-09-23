package org.ops4j.pax.construct.lifecycle;

/*
 * Copyright 2007 Stuart McCulloch
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

import java.io.File;
import java.util.List;

import org.apache.maven.plugin.CompilationFailureException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.TestCompilerMojo;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.ops4j.pax.construct.util.DirUtils;

/**
 * @extendsPlugin compiler
 * @goal testCompile
 * @phase test-compile
 * @requiresDependencyResolution test
 */
public class BundleTestCompilerMojo extends TestCompilerMojo
{
    /**
     * @component
     */
    ArchiverManager archiverManager;

    protected List getClasspathElements()
    {
        File tempDir = getOutputDirectory().getParentFile();

        return DirUtils.expandBundleClassPath( super.getClasspathElements(), archiverManager, tempDir );
    }

    public void execute()
        throws MojoExecutionException,
        CompilationFailureException
    {
        try
        {
            super.execute();
        }
        catch( CompilationFailureException e )
        {
            SqueakyCleanMojo.recoverMetaData( this );

            throw e;
        }
    }
}
