/**
 * Copyright 2015-2017 Emmanuel Keller / QWAZR
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package com.qwazr.library.iban;

import com.qwazr.library.annotations.Library;
import com.qwazr.library.test.AbstractLibraryTest;
import org.junit.Assert;
import org.junit.Test;

public class IbanToolTest extends AbstractLibraryTest {

	@Library("iban")
	private IbanTool iban;

	@Test
	public void testDefault() {
		Assert.assertNotNull(iban);
		Assert.assertNull(iban.iban_validate_default("DE89 3704 0044 0532 0130 00"));
	}

	@Test
	public void testCompact() {
		Assert.assertNotNull(iban);
		Assert.assertNull(iban.iban_validate_compact("DE89370400440532013000"));
	}

	@Test
	public void testBic() {
		Assert.assertNotNull(iban);
		Assert.assertNull(iban.bic_validate("OKOYFIHH"));
	}

}
