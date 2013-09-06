package com.rayo.core.sip;

import javax.annotation.Resource;
import javax.servlet.sip.Address;
import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipFactory;

public class SipAddress {

	@Resource
	private SipFactory sip;

	private Address _address;

	public SipAddress(String address) {
		try {
			_address = sip.createAddress(address);
		} catch (ServletParseException e) {
			throw new IllegalArgumentException(e.getMessage());
		}
	}

	public String getUri() {
		return _address.getURI().toString();
	}
}
