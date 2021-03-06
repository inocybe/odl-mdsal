/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.sal.java.api.generator.stmt.parser.retest;

import static org.junit.Assert.assertTrue;
import static org.opendaylight.yangtools.sal.java.api.generator.stmt.parser.retest.CompilationTestUtils.COMPILER_OUTPUT_DIR;
import static org.opendaylight.yangtools.sal.java.api.generator.stmt.parser.retest.CompilationTestUtils.GENERATOR_OUTPUT_DIR;
import static org.opendaylight.yangtools.sal.java.api.generator.stmt.parser.retest.CompilationTestUtils.TEST_DIR;
import static org.opendaylight.yangtools.sal.java.api.generator.stmt.parser.retest.CompilationTestUtils.deleteTestDir;
import org.junit.Before;
import org.junit.BeforeClass;
import org.opendaylight.yangtools.sal.binding.generator.api.BindingGenerator;
import org.opendaylight.yangtools.sal.binding.generator.impl.BindingGeneratorImpl;

public abstract class BaseCompilationTest {

    protected BindingGenerator bindingGenerator;

    @BeforeClass
    public static void createTestDirs() {
        if (TEST_DIR.exists()) {
            deleteTestDir(TEST_DIR);
        }
        assertTrue(GENERATOR_OUTPUT_DIR.mkdirs());
        assertTrue(COMPILER_OUTPUT_DIR.mkdirs());
    }

    @Before
    public void init() {
        bindingGenerator = new BindingGeneratorImpl(true);
    }

}
