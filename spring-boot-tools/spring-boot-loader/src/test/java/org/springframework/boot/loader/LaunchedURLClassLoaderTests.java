/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.loader;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.springframework.boot.loader.jar.JarFile;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LaunchedURLClassLoader}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
@SuppressWarnings("resource")
public class LaunchedURLClassLoaderTests {

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Test
	public void resolveResourceFromArchive() throws Exception {
		LaunchedURLClassLoader loader = new LaunchedURLClassLoader(
				new URL[] { new URL("jar:file:src/test/resources/jars/app.jar!/") },
				getClass().getClassLoader());
		assertThat(loader.getResource("demo/Application.java")).isNotNull();
	}

	@Test
	public void resolveResourcesFromArchive() throws Exception {
		LaunchedURLClassLoader loader = new LaunchedURLClassLoader(
				new URL[] { new URL("jar:file:src/test/resources/jars/app.jar!/") },
				getClass().getClassLoader());
		assertThat(loader.getResources("demo/Application.java").hasMoreElements())
				.isTrue();
	}

	@Test
	public void resolveRootPathFromArchive() throws Exception {
		LaunchedURLClassLoader loader = new LaunchedURLClassLoader(
				new URL[] { new URL("jar:file:src/test/resources/jars/app.jar!/") },
				getClass().getClassLoader());
		assertThat(loader.getResource("")).isNotNull();
	}

	@Test
	public void resolveRootResourcesFromArchive() throws Exception {
		LaunchedURLClassLoader loader = new LaunchedURLClassLoader(
				new URL[] { new URL("jar:file:src/test/resources/jars/app.jar!/") },
				getClass().getClassLoader());
		assertThat(loader.getResources("").hasMoreElements()).isTrue();
	}

	@Test
	public void resolveFromNested() throws Exception {
		File file = this.temporaryFolder.newFile();
		TestJarCreator.createTestJar(file);
		JarFile jarFile = new JarFile(file);
		URL url = jarFile.getUrl();
		LaunchedURLClassLoader loader = new LaunchedURLClassLoader(new URL[] { url },
				null);
		URL resource = loader.getResource("nested.jar!/3.dat");
		assertThat(resource.toString()).isEqualTo(url + "nested.jar!/3.dat");
		assertThat(resource.openConnection().getInputStream().read()).isEqualTo(3);
	}

	@Test
	public void resolveFromNestingNestedJarRelativePath() throws Exception {
		File file = this.temporaryFolder.newFile();
		TestJarCreator.createTestJar(file, false, true);
		JarFile jarFile = new JarFile(file);
		JarFile nestingNestedJarFile = jarFile.getNestedJarFile(jarFile.getEntry("nesting-nested.jar"));
		JarFile otherJarFile = jarFile.getNestedJarFile(jarFile.getEntry("n123456789012345678901234567890.jar"));
		LaunchedURLClassLoader loader = new LaunchedURLClassLoader(
				new URL[] { otherJarFile.getUrl(), nestingNestedJarFile.getUrl() }, null);
		String relativePath = "nested.jar!/3.dat";
		URL resource = loader.getResource(relativePath);
		System.out.println("Looked for: " + relativePath);
		System.out.println("Found resource: " + resource);
		assertThat(resource.toString()).isEqualTo(nestingNestedJarFile.getUrl() + relativePath);
		assertThat(resource.openConnection().getInputStream().read()).isEqualTo(3);
	}

	@Test
	public void resolveFromNestingNestedJarRelativePathWithSpace() throws Exception {
		File file = this.temporaryFolder.newFile();
		TestJarCreator.createTestJar(file, false, true);
		JarFile jarFile = new JarFile(file);
		JarFile nestingNestedJarFile = jarFile.getNestedJarFile(jarFile.getEntry("nesting nested.jar"));
		JarFile otherJarFile = jarFile.getNestedJarFile(jarFile.getEntry("n123456789012345678901234567890.jar"));
		LaunchedURLClassLoader loader = new LaunchedURLClassLoader(
				new URL[] { otherJarFile.getUrl(), nestingNestedJarFile.getUrl() }, null);
		String relativePath = "nested.jar!/3.dat";
		URL resource = loader.getResource(relativePath);
		System.out.println("Looked for: " + relativePath);
		System.out.println("Found resource: " + resource);
		// TODO why there are spaces in the resource URL?
		// e.g. Found resource: jar:file:/C:/Users/.../junit6485939145488725358.tmp!/nesting nested.jar!/nested.jar!/3.dat
		assertThat(resource.toString()).isEqualTo(decodedUrl(nestingNestedJarFile) + relativePath);
		assertThat(resource.openConnection().getInputStream().read()).isEqualTo(3);
	}

	@Test
	public void resolveFromNestingNestedJarRelativePathWithDollarSign() throws Exception {
		File file = this.temporaryFolder.newFile();
		TestJarCreator.createTestJar(file, false, true);
		JarFile jarFile = new JarFile(file);
		JarFile nestingNestedJarFile = jarFile.getNestedJarFile(jarFile.getEntry("nesting$nested.jar"));
		JarFile otherJarFile = jarFile.getNestedJarFile(jarFile.getEntry("n123456789012345678901234567890.jar"));
		LaunchedURLClassLoader loader = new LaunchedURLClassLoader(
				new URL[] { otherJarFile.getUrl(), nestingNestedJarFile.getUrl() }, null);
		String relativePath = "nested.jar!/3.dat";
		URL resource = loader.getResource(relativePath);
		System.out.println("Looked for: " + relativePath);
		System.out.println("Found resource: " + resource);
		assertThat(resource.toString()).isEqualTo(decodedUrl(nestingNestedJarFile) + relativePath);
		assertThat(resource.openConnection().getInputStream().read()).isEqualTo(3);
	}

	@Test
	public void resolveFromNestingNestedJarRelativePathWithPlusSign() throws Exception {
		File file = this.temporaryFolder.newFile();
		TestJarCreator.createTestJar(file, false, true);
		JarFile jarFile = new JarFile(file);
		JarFile nestingNestedJarFile = jarFile.getNestedJarFile(jarFile.getEntry("nesting+nested.jar"));
		JarFile otherJarFile = jarFile.getNestedJarFile(jarFile.getEntry("n123456789012345678901234567890.jar"));
		LaunchedURLClassLoader loader = new LaunchedURLClassLoader(
				new URL[] { otherJarFile.getUrl(), nestingNestedJarFile.getUrl() }, null);
		String relativePath = "nested.jar!/3.dat";
		URL resource = loader.getResource(relativePath);
		System.out.println("Looked for: " + relativePath);
		System.out.println("Found resource: " + resource);
		assertThat(resource.toString()).isEqualTo(nestingNestedJarFile.getUrl() + relativePath);
		assertThat(resource.openConnection().getInputStream().read()).isEqualTo(3);
	}

	@Test
	public void resolveFromNestingNestedJarRelativePathWithTwoSpaces() throws Exception {
		File file = this.temporaryFolder.newFile();
		TestJarCreator.createTestJar(file, false, true);
		JarFile jarFile = new JarFile(file);
		JarFile nestingNestedJarFile = jarFile.getNestedJarFile(jarFile.getEntry("nesting nested 2.jar"));
		JarFile otherJarFile = jarFile.getNestedJarFile(jarFile.getEntry("n123456789012345678901234567890.jar"));
		LaunchedURLClassLoader loader = new LaunchedURLClassLoader(
				new URL[] { otherJarFile.getUrl(), nestingNestedJarFile.getUrl() }, null);
		String relativePath = "nested 2.jar!/3.dat";
		URL resource = loader.getResource(relativePath);
		System.out.println("Looked for: " + relativePath);
		System.out.println("Found resource: " + resource);
		// TODO why there are spaces in the resource URL?
		// e.g. Found resource: jar:file:/C:/Users/.../junit2697888914968958974.tmp!/nesting nested 2.jar!/nested%202.jar!/3.dat
		assertThat(resource.toString()).isEqualTo(decodedUrl(nestingNestedJarFile) + relativePath.replace(" ", "%20"));
		assertThat(resource.openConnection().getInputStream().read()).isEqualTo(3);
	}

	@Test
	public void resolveFromNestedNestedJarAbsolutePath() throws Exception {
		File file = this.temporaryFolder.newFile();
		TestJarCreator.createTestJar(file, false, true);
		JarFile jarFile = new JarFile(file);
		JarFile nestedJarFile = jarFile.getNestedJarFile(jarFile.getEntry("nesting-nested.jar"));
		JarFile otherJarFile = jarFile.getNestedJarFile(jarFile.getEntry("n123456789012345678901234567890.jar"));
		LaunchedURLClassLoader loader = new LaunchedURLClassLoader(new URL[] { otherJarFile.getUrl(), nestedJarFile.getUrl() },
				null);
		System.out.println(otherJarFile.getUrl());
		System.out.println(nestedJarFile.getUrl());
		String absolutePath = nestedJarFile.getUrl() + "nested.jar!/3.dat";
		URL resource = loader.getResource(absolutePath);
		System.out.println("Looked for: " + absolutePath);
		System.out.println("Found resource: " + resource);
		assertThat(resource.toString()).isEqualTo(absolutePath);
		assertThat(resource.openConnection().getInputStream().read()).isEqualTo(3);
	}

	@Test
	public void resolveFromNestedNestedJarAbsolutePathWithSpace() throws Exception {
		File file = this.temporaryFolder.newFile();
		TestJarCreator.createTestJar(file, false, true);
		JarFile jarFile = new JarFile(file);
		JarFile nestedJarFile = jarFile.getNestedJarFile(jarFile.getEntry("nesting nested.jar"));
		JarFile otherJarFile = jarFile.getNestedJarFile(jarFile.getEntry("n123456789012345678901234567890.jar"));
		LaunchedURLClassLoader loader = new LaunchedURLClassLoader(new URL[] { otherJarFile.getUrl(), nestedJarFile.getUrl() },
				null);
		String absolutePath = nestedJarFile.getUrl() + "nested.jar!/3.dat";
		URL resource = loader.getResource(absolutePath);
		System.out.println("Looked for: " + absolutePath);
		System.out.println("Found resource: " + resource);
		assertThat(resource.toString()).isEqualTo(absolutePath.replace(" ", "%20"));
		assertThat(resource.openConnection().getInputStream().read()).isEqualTo(3);
	}

	@Test
	public void resolveFromNestedNestedJarAbsolutePathWithTwoSpaces() throws Exception {
		File file = this.temporaryFolder.newFile();
		TestJarCreator.createTestJar(file, false, true);
		JarFile jarFile = new JarFile(file);
		JarFile nestedJarFile = jarFile.getNestedJarFile(jarFile.getEntry("nesting nested 2.jar"));
		JarFile otherJarFile = jarFile.getNestedJarFile(jarFile.getEntry("n123456789012345678901234567890.jar"));
		LaunchedURLClassLoader loader = new LaunchedURLClassLoader(new URL[] { otherJarFile.getUrl(), nestedJarFile.getUrl() },
				null);
		String absolutePath = nestedJarFile.getUrl() + "nested 2.jar!/3.dat";
		URL resource = loader.getResource(absolutePath);
		System.out.println("Looked for: " + absolutePath);
		System.out.println("Found resource: " + resource);
		assertThat(resource.toString()).isEqualTo(absolutePath.replace(" ", "%20"));
		assertThat(resource.openConnection().getInputStream().read()).isEqualTo(3);
	}

	@Test
	public void resolveFromNestedNestedJarAbsolutePathWithDollarSign() throws Exception {
		File file = this.temporaryFolder.newFile();
		TestJarCreator.createTestJar(file, false, true);
		JarFile jarFile = new JarFile(file);
		JarFile nestedJarFile = jarFile.getNestedJarFile(jarFile.getEntry("nesting$nested.jar"));
		JarFile otherJarFile = jarFile.getNestedJarFile(jarFile.getEntry("n123456789012345678901234567890.jar"));
		LaunchedURLClassLoader loader = new LaunchedURLClassLoader(new URL[] { otherJarFile.getUrl(), nestedJarFile.getUrl() },
				null);
		String absolutePath = nestedJarFile.getUrl() + "nested.jar!/3.dat";
		URL resource = loader.getResource(absolutePath);
		System.out.println("Looked for: " + absolutePath);
		System.out.println("Found resource: " + resource);
		assertThat(resource.toString()).isEqualTo(absolutePath);
		assertThat(resource.openConnection().getInputStream().read()).isEqualTo(3);
	}

	@Test
	public void resolveFromNestedWhileThreadIsInterrupted() throws Exception {
		File file = this.temporaryFolder.newFile();
		TestJarCreator.createTestJar(file);
		JarFile jarFile = new JarFile(file);
		URL url = jarFile.getUrl();
		LaunchedURLClassLoader loader = new LaunchedURLClassLoader(new URL[] { url },
				null);
		try {
			Thread.currentThread().interrupt();
			URL resource = loader.getResource("nested.jar!/3.dat");
			assertThat(resource.toString()).isEqualTo(url + "nested.jar!/3.dat");
			assertThat(resource.openConnection().getInputStream().read()).isEqualTo(3);
		}
		finally {
			Thread.interrupted();
		}
	}

	private String decodedUrl(JarFile jarFile) throws MalformedURLException {
		try {
			return URLDecoder.decode(jarFile.getUrl().toString(), "utf-8");
		}
		catch (UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
	}
}
