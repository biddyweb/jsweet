/* 
 * JSweet - http://www.jsweet.org
 * Copyright (C) 2015 CINCHEO SAS <renaud.pawlak@cincheo.fr>
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jsweet.test.transpiler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsweet.JSweetCommandLineLauncher;
import org.jsweet.transpiler.JSweetContext;
import org.jsweet.transpiler.JSweetFactory;
import org.jsweet.transpiler.JSweetTranspiler;
import org.jsweet.transpiler.ModuleKind;
import org.jsweet.transpiler.SourceFile;
import org.jsweet.transpiler.SourcePosition;
import org.jsweet.transpiler.util.ProcessUtil;
import org.jsweet.transpiler.util.Util;
import org.junit.Ignore;
import org.junit.Test;

import source.blocksgame.Ball;
import source.blocksgame.BlockElement;
import source.blocksgame.Factory;
import source.blocksgame.GameArea;
import source.blocksgame.GameManager;
import source.blocksgame.Globals;
import source.blocksgame.Player;
import source.blocksgame.util.AnimatedElement;
import source.blocksgame.util.Collisions;
import source.blocksgame.util.Direction;
import source.blocksgame.util.Line;
import source.blocksgame.util.MobileElement;
import source.blocksgame.util.Point;
import source.blocksgame.util.Rectangle;
import source.blocksgame.util.Vector;
import source.overload.Overload;
import source.structural.AbstractClass;
import source.transpiler.CanvasDrawing;

public class TranspilerTests extends AbstractTest {

	@Ignore
	@Test
	public void testTranspilerWatchMode() {
		TestTranspilationHandler logHandler = new TestTranspilationHandler();
		try {
			File overload = getSourceFile(Overload.class).getJavaFile();
			File abstractClass = getSourceFile(AbstractClass.class).getJavaFile();
			JSweetTranspiler<JSweetContext> transpiler = new JSweetTranspiler<>(new JSweetFactory<>());
			transpiler.setTscWatchMode(true);
			transpiler.setPreserveSourceLineNumbers(true);
			long t = System.currentTimeMillis();
			transpiler.transpile(logHandler, new SourceFile(overload), new SourceFile(abstractClass));
			t = System.currentTimeMillis() - t;
			assertEquals("There should be no problems", 0, logHandler.reportedProblems.size());
			assertTrue("Wrong transpiler state", transpiler.isTscWatchMode());
			assertEquals("Wrong transpiler state", 2, transpiler.getWatchedFiles().length);
			assertEquals("Wrong transpiler state", overload, transpiler.getWatchedFile(overload).getJavaFile());

			Thread.sleep(Math.max(4000, t * 5));

			assertTrue("File not generated", transpiler.getWatchedFile(overload).getJsFile().exists());

			long ts1 = transpiler.getWatchedFile(overload).getJsFileLastTranspiled();

			transpiler.getWatchedFile(overload).getTsFile().setLastModified(System.currentTimeMillis());

			Thread.sleep(Math.max(2000, t * 4));

			assertTrue("File not regenerated", transpiler.getWatchedFile(overload).getJsFileLastTranspiled() != ts1);

			transpiler.transpile(logHandler, new SourceFile(overload));
			assertEquals("There should be no problems", 0, logHandler.reportedProblems.size());
			assertTrue("Wrong transpiler state", transpiler.isTscWatchMode());
			assertEquals("Wrong transpiler state", 2, transpiler.getWatchedFiles().length);
			assertEquals("Wrong transpiler state", overload, transpiler.getWatchedFile(overload).getJavaFile());

			Thread.sleep(Math.max(2000, t * 4));

			assertTrue("File not regenerated", transpiler.getWatchedFile(overload).getJsFileLastTranspiled() != ts1);

			transpiler.resetTscWatchMode();

			transpiler.transpile(logHandler, new SourceFile(overload));
			assertEquals("There should be no problems", 0, logHandler.reportedProblems.size());
			assertTrue("Wrong transpiler state", transpiler.isTscWatchMode());
			assertEquals("Wrong transpiler state", 1, transpiler.getWatchedFiles().length);
			assertEquals("Wrong transpiler state", overload, transpiler.getWatchedFile(overload).getJavaFile());

			Thread.sleep(Math.max(1000, t * 2));

			assertTrue("File not regenerated", transpiler.getWatchedFile(overload).getJsFileLastTranspiled() != ts1);

			transpiler.setTscWatchMode(false);
			SourceFile sf = new SourceFile(overload);
			transpiler.transpile(logHandler, sf);
			assertEquals("There should be no problems", 0, logHandler.reportedProblems.size());
			assertTrue("Wrong transpiler state", !transpiler.isTscWatchMode());
			assertTrue("Wrong transpiler state", transpiler.getWatchedFiles() == null);

			ts1 = sf.getJsFileLastTranspiled();

			sf.getTsFile().setLastModified(System.currentTimeMillis());

			Thread.sleep(Math.max(1000, t * 2));

			assertTrue("File regenerated", sf.getJsFileLastTranspiled() == ts1);

		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception occured while running test");
		}
	}

	@Ignore
	@Test
	public void testTscInstallation() throws Exception {
		ProcessUtil.uninstallNodePackage("typescript", true);
		assertFalse(ProcessUtil.isInstalledWithNpm("tsc"));
		globalSetUp();
		// transpiler.cleanWorkingDirectory();
		transpile(ModuleKind.none, h -> h.assertNoProblems(), getSourceFile(Overload.class));
	}

	@Test
	public void testCommandLine() throws Throwable {
		File outDir = new File(new File(TMPOUT_DIR), getCurrentTestName() + "/" + ModuleKind.none);

		Process process = ProcessUtil.runCommand("java", line -> {
			System.out.println(line);
		}, null, "-cp", System.getProperty("java.class.path"), //
				JSweetCommandLineLauncher.class.getName(), //
				"--tsout", outDir.getPath(), //
				"--jsout", outDir.getPath(), //
				"--sourceMap", //
				"-i", TEST_DIRECTORY_NAME + "/org/jsweet/test/transpiler/source/blocksgame");

		assertTrue(process.exitValue() == 0);
		LinkedList<File> files = new LinkedList<>();
		Util.addFiles(".ts", outDir, files);
		assertTrue(!files.isEmpty());
		Util.addFiles(".js", outDir, files);
		assertTrue(!files.isEmpty());
		Util.addFiles(".js.map", outDir, files);
		assertTrue(!files.isEmpty());

	}

	@Test
	public void testSourceMapsSimple() throws Throwable {
		boolean sourceMaps = transpiler.isPreserveSourceLineNumbers();
		try {
			transpiler.setPreserveSourceLineNumbers(true);
			SourceFile[] sourceFiles = { getSourceFile(CanvasDrawing.class) };
			transpile(ModuleKind.none, logHandler -> {

				logger.info("transpilation finished: " + transpiler.getModuleKind());
				logHandler.assertNoProblems();
				logger.info(sourceFiles[0].getSourceMap().toString());

				assertEqualPositions(sourceFiles, sourceFiles[0], "angle += 0.05");
				assertEqualPositions(sourceFiles, sourceFiles[0], "aTestVar");
				assertEqualPositions(sourceFiles, sourceFiles[0], "for");

				assertEqualPositions(sourceFiles, sourceFiles[0], "aTestParam1");
				assertEqualPositions(sourceFiles, sourceFiles[0], "aTestParam2");

				assertEqualPositions(sourceFiles, sourceFiles[0], "aTestString");

			}, Arrays.copyOf(sourceFiles, sourceFiles.length));
		} finally {
			transpiler.setPreserveSourceLineNumbers(sourceMaps);
		}
	}

	@Test
	public void testSourceMaps() throws Throwable {
		boolean sourceMaps = transpiler.isPreserveSourceLineNumbers();
		try {
			transpiler.setPreserveSourceLineNumbers(true);
			SourceFile[] sourceFiles = { getSourceFile(Point.class), getSourceFile(Vector.class),
					getSourceFile(AnimatedElement.class), getSourceFile(Line.class), getSourceFile(MobileElement.class),
					getSourceFile(Rectangle.class), getSourceFile(Direction.class), getSourceFile(Collisions.class),
					getSourceFile(Ball.class), getSourceFile(Globals.class), getSourceFile(BlockElement.class),
					getSourceFile(Factory.class), getSourceFile(GameArea.class), getSourceFile(GameManager.class),
					getSourceFile(Player.class) };
			transpile(logHandler -> {

				logger.info("transpilation finished: " + transpiler.getModuleKind());
				logHandler.assertNoProblems();
				logger.info(sourceFiles[11].getSourceMap().toString());

				assertEqualPositions(sourceFiles, sourceFiles[0], " distance(");
				assertEqualPositions(sourceFiles, sourceFiles[0], "class Point");
				assertEqualPositions(sourceFiles, sourceFiles[3], "invalid query on non-vertical lines");

				assertEqualPositions(sourceFiles, sourceFiles[11], "class InnerClass");
				assertEqualPositions(sourceFiles, sourceFiles[11], "inner class");

			}, Arrays.copyOf(sourceFiles, sourceFiles.length));
		} finally {
			transpiler.setPreserveSourceLineNumbers(sourceMaps);
		}
	}

	private void assertEqualPositions(SourceFile[] sourceFiles, SourceFile sourceFile, String codeSnippet) {
		assertEqualPositions(sourceFiles, sourceFile, codeSnippet, codeSnippet);
	}

	private void assertEqualPositions(SourceFile[] sourceFiles, SourceFile sourceFile, String javaCodeSnippet,
			String tsCodeSnippet) {
		logger.info("assert equal positions for '" + javaCodeSnippet + "' -> '" + tsCodeSnippet + "'");
		SourcePosition tsPosition = getPosition(sourceFile.getTsFile(), tsCodeSnippet);
		SourcePosition javaPosition = SourceFile.findOriginPosition(tsPosition, Arrays.asList(sourceFiles));
		logger.info("org: " + javaPosition + " --> " + tsPosition);
		assertEquals(getPosition(sourceFile.getJavaFile(), javaCodeSnippet).getStartLine(),
				javaPosition.getStartLine());
	}

	private SourcePosition getPosition(File f, String codeSnippet) {
		try {
			String s1 = FileUtils.readFileToString(f);
			int index = s1.indexOf(codeSnippet);
			if (index == -1) {
				System.out.println("DO NOT FIND: " + codeSnippet);
				System.out.println(s1);
			}
			String s = s1.substring(0, index);
			return new SourcePosition(f, null, StringUtils.countMatches(s, "\n") + 1,
					s.length() - s.lastIndexOf("\n") - 1);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
