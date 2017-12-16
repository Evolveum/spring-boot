/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.loader.jar;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import org.springframework.boot.loader.TestJarCreator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JarURLConnection}.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Rostyslav Dudka
 */
public class JarURLConnectionTests {

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder(new File("target"));

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private File rootJarFile;

	private JarFile jarFile;

	@Before
	public void setup() throws Exception {
		this.rootJarFile = this.temporaryFolder.newFile();
		TestJarCreator.createTestJar(this.rootJarFile, false, true);
		this.jarFile = new JarFile(this.rootJarFile);
	}

	@Test
	public void connectionToRootUsingAbsoluteUrl() throws Exception {
		URL url = new URL("jar:file:" + getAbsolutePath() + "!/");
		assertThat(JarURLConnection.get(url, this.jarFile).getContent())
				.isSameAs(this.jarFile);
	}

	@Test
	public void connectionToRootUsingRelativeUrl() throws Exception {
		URL url = new URL("jar:file:" + getRelativePath() + "!/");
		assertThat(JarURLConnection.get(url, this.jarFile).getContent())
				.isSameAs(this.jarFile);
	}

	@Test
	public void connectionToEntryUsingAbsoluteUrl() throws Exception {
		URL url = new URL("jar:file:" + getAbsolutePath() + "!/1.dat");
		assertThat(JarURLConnection.get(url, this.jarFile).getInputStream())
				.hasSameContentAs(new ByteArrayInputStream(new byte[] { 1 }));
	}

	@Test
	public void connectionToEntryUsingRelativeUrl() throws Exception {
		URL url = new URL("jar:file:" + getRelativePath() + "!/1.dat");
		assertThat(JarURLConnection.get(url, this.jarFile).getInputStream())
				.hasSameContentAs(new ByteArrayInputStream(new byte[] { 1 }));
	}

	@Test
	public void connectionToEntryUsingAbsoluteUrlWithFileColonSlashSlashPrefix()
			throws Exception {
		URL url = new URL("jar:file:/" + getAbsolutePath() + "!/1.dat");
		assertThat(JarURLConnection.get(url, this.jarFile).getInputStream())
				.hasSameContentAs(new ByteArrayInputStream(new byte[] { 1 }));
	}

	@Test
	public void connectionToEntryUsingAbsoluteUrlForNestedEntry() throws Exception {
		URL url = new URL("jar:file:" + getAbsolutePath() + "!/nested.jar!/3.dat");
		assertThat(JarURLConnection.get(url, this.jarFile).getInputStream())
				.hasSameContentAs(new ByteArrayInputStream(new byte[] { 3 }));
	}

	@Test(expected = FileNotFoundException.class)
	public void connectionToEntryUsingWrongAbsoluteUrlForNestedEntry() throws Exception {
		URL url = new URL("jar:file:" + getAbsolutePath() + "!/nested-wrong.jar!/3.dat");
		JarURLConnection.get(url, this.jarFile).getInputStream();
	}

	@Test(expected = FileNotFoundException.class)
	public void connectionToEntryUsingWrongAbsoluteUrlForNestedEntry2() throws Exception {
		URL url = new URL("jar:file:" + getAbsolutePath() + "!/nested.jar!/3-wrong.dat");
		JarURLConnection.get(url, this.jarFile).getInputStream();
	}

	@Test
	public void connectionToEntryUsingRelativeUrlForNestedEntry() throws Exception {
		URL url = new URL("jar:file:" + getRelativePath() + "!/nested.jar!/3.dat");
		assertThat(JarURLConnection.get(url, this.jarFile).getInputStream())
				.hasSameContentAs(new ByteArrayInputStream(new byte[] { 3 }));
	}

	@Test
	public void connectionToEntryUsingRelativeUrlForNestedNestedEntry() throws Exception {
		URL url = new URL("jar:file:" + getRelativePath() + "!/nesting-nested.jar!/nested.jar!/3.dat");
		assertThat(JarURLConnection.get(url, this.jarFile).getInputStream())
				.hasSameContentAs(new ByteArrayInputStream(new byte[] { 3 }));
	}

	@Test
	public void connectionToEntryUsingRelativeUrlForNestedNestedEntryWithSpace() throws Exception {
		URL url = new URL("jar:file:" + getRelativePath() + "!/nesting%20nested.jar!/nested.jar!/3.dat");
		assertThat(JarURLConnection.get(url, this.jarFile).getInputStream())
				.hasSameContentAs(new ByteArrayInputStream(new byte[] { 3 }));
	}

	@Test
	public void connectionToEntryUsingRelativeUrlForNestedNestedEntryWithTwoSpaces() throws Exception {
		URL url = new URL("jar:file:" + getRelativePath() + "!/nesting%20nested%202.jar!/nested%202.jar!/3.dat");
		assertThat(JarURLConnection.get(url, this.jarFile).getInputStream())
				.hasSameContentAs(new ByteArrayInputStream(new byte[] { 3 }));
	}

	@Test
	public void connectionToEntryUsingRelativeUrlForNestedNestedEntryFromNestedJarFile() throws Exception {
		URL url = new URL("jar:file:" + getRelativePath() + "!/nesting-nested.jar!/nested.jar!/3.dat");
		JarFile nested = this.jarFile
				.getNestedJarFile(this.jarFile.getEntry("nesting-nested.jar"));
		assertThat(JarURLConnection.get(url, nested).getInputStream())
				.hasSameContentAs(new ByteArrayInputStream(new byte[] { 3 }));
	}

	@Test
	public void connectionToEntryUsingRelativeUrlForNestedNestedEntryFromNested2JarFile() throws Exception {
		URL url = new URL("jar:file:" + getRelativePath() + "!/nesting-nested.jar!/nested.jar!/3.dat");
		JarFile nested = this.jarFile
				.getNestedJarFile(this.jarFile.getEntry("nesting-nested.jar"));
		JarFile nested2 = nested
				.getNestedJarFile(this.jarFile.getEntry("nested.jar"));
		assertThat(JarURLConnection.get(url, nested2).getInputStream())
				.hasSameContentAs(new ByteArrayInputStream(new byte[] { 3 }));
	}

	@Test
	public void connectionToEntryUsingAbsoluteUrlForEntryFromNestedJarFile()
			throws Exception {
		URL url = new URL("jar:file:" + getAbsolutePath() + "!/nested.jar!/3.dat");
		JarFile nested = this.jarFile
				.getNestedJarFile(this.jarFile.getEntry("nested.jar"));
		assertThat(JarURLConnection.get(url, nested).getInputStream())
				.hasSameContentAs(new ByteArrayInputStream(new byte[] { 3 }));
	}

	@Test(expected = FileNotFoundException.class)
	public void connectionToEntryUsingWrongAbsoluteUrlForEntryFromNestedJarFile()
			throws Exception {
		// coincidental match (extractFullSpec would return "")
		URL url = new URL("jar:file:" + getAbsolutePath() + "!/w.jar!/3.dat");
		JarFile nested = this.jarFile
				.getNestedJarFile(this.jarFile.getEntry("nested.jar"));
		JarURLConnection.get(url, nested).getInputStream();
	}

	@Test(expected = FileNotFoundException.class)
	public void connectionToEntryUsingWrongAbsoluteUrlForEntryFromNestedJarFile2()
			throws Exception {
		URL url = new URL("jar:file:" + getAbsolutePath() + "!/nested.jar!/3-wrong.dat");
		JarFile nested = this.jarFile
				.getNestedJarFile(this.jarFile.getEntry("nested.jar"));
		JarURLConnection.get(url, nested).getInputStream();
	}

	@Test
	public void connectionToEntryUsingRelativeUrlForEntryFromNestedJarFile()
			throws Exception {
		URL url = new URL("jar:file:" + getRelativePath() + "!/nested.jar!/3.dat");
		JarFile nested = this.jarFile
				.getNestedJarFile(this.jarFile.getEntry("nested.jar"));
		assertThat(JarURLConnection.get(url, nested).getInputStream())
				.hasSameContentAs(new ByteArrayInputStream(new byte[] { 3 }));
	}

	@Test
	public void connectionToEntryInNestedJarFromUrlThatUsesExistingUrlAsContext()
			throws Exception {
		URL url = new URL(new URL("jar", null, -1,
				"file:" + getAbsolutePath() + "!/nested.jar!/", new Handler()), "/3.dat");
		JarFile nested = this.jarFile
				.getNestedJarFile(this.jarFile.getEntry("nested.jar"));
		assertThat(JarURLConnection.get(url, nested).getInputStream())
				.hasSameContentAs(new ByteArrayInputStream(new byte[] { 3 }));
	}

	@Test
	public void getContentLengthReturnsLengthOfUnderlyingEntry() throws Exception {
		URL url = new URL(new URL("jar", null, -1,
				"file:" + getAbsolutePath() + "!/nested.jar!/", new Handler()), "/3.dat");
		assertThat(url.openConnection().getContentLength()).isEqualTo(1);
	}

	@Test
	public void getContentLengthLongReturnsLengthOfUnderlyingEntry() throws Exception {
		URL url = new URL(new URL("jar", null, -1,
				"file:" + getAbsolutePath() + "!/nested.jar!/", new Handler()), "/3.dat");
		assertThat(url.openConnection().getContentLengthLong()).isEqualTo(1);
	}

	@Test
	public void getLastModifiedReturnsLastModifiedTimeOfJarEntry() throws Exception {
		URL url = new URL("jar:file:" + getAbsolutePath() + "!/1.dat");
		JarURLConnection connection = JarURLConnection.get(url, this.jarFile);
		assertThat(connection.getLastModified())
				.isEqualTo(connection.getJarEntry().getTime());
	}

	private String getAbsolutePath() {
		return this.rootJarFile.getAbsolutePath().replace('\\', '/');
	}

	private String getRelativePath() {
		return this.rootJarFile.getPath().replace('\\', '/');
	}

}
