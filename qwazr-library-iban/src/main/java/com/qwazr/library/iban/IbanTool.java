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

import com.qwazr.library.AbstractLibrary;
import com.qwazr.utils.StringUtils;
import org.iban4j.*;

import java.util.Map;

public class IbanTool extends AbstractLibrary {

	public final Map<String, String> error_messages = null;

	private String getErrorMessage(final String key, final Iban4jException error) {
		if (error_messages == null)
			return error.getLocalizedMessage();
		String msg = error_messages.get(key);
		return StringUtils.isEmpty(msg) ? error.getLocalizedMessage() : msg;
	}

	public String iban_validate_compact(final String iban) {
		try {
			IbanUtil.validate(iban);
			return null;
		} catch (IbanFormatException e) {
			return getErrorMessage(e.getFormatViolation().name(), e);
		} catch (InvalidCheckDigitException e) {
			return getErrorMessage("INVALIDCHECKDIGIT", e);
		} catch (UnsupportedCountryException e) {
			return getErrorMessage("UNSUPPORTEDCOUNTRY", e);
		}
	}

	public String iban_validate_default(final String iban) {
		try {
			IbanUtil.validate(iban, IbanFormat.Default);
			return null;
		} catch (IbanFormatException e) {
			return getErrorMessage(e.getFormatViolation().name(), e);
		} catch (InvalidCheckDigitException e) {
			return getErrorMessage("INVALIDCHECKDIGIT", e);
		} catch (UnsupportedCountryException e) {
			return getErrorMessage("UNSUPPORTEDCOUNTRY", e);
		}
	}

	public String bic_validate(final String bic) {
		try {
			BicUtil.validate(bic);
			return null;
		} catch (BicFormatException e) {
			return getErrorMessage(e.getFormatViolation().name(), e);
		} catch (UnsupportedCountryException e) {
			return getErrorMessage("UNSUPPORTEDCOUNTRY", e);
		}
	}

}
