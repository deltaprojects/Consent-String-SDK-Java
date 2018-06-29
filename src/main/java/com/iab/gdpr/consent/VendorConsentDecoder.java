package com.iab.gdpr.consent;

import com.iab.gdpr.Bits;
import com.iab.gdpr.consent.implementation.v1.ByteBufferBackedVendorConsent;
import com.iab.gdpr.exception.VendorConsentParseException;

import java.util.Base64;

import static com.iab.gdpr.GdprConstants.*;

/**
 * {@link VendorConsent} decoder from Base64 string. Right now only version 1 is know, but eventually
 * this can be extended to support new versions
 */
public class VendorConsentDecoder {

    private static final Base64.Decoder BASE64_DECODER = Base64.getUrlDecoder();

    public static VendorConsent fromBase64String(String consentString) {
        if (isNullOrEmpty(consentString))
            throw new IllegalArgumentException("Null or empty consent string passed as an argument");

        return fromByteArray(BASE64_DECODER.decode(consentString));
    }

    public static VendorConsent fromByteArray(byte[] bytes) {
        if (bytes == null || bytes.length == 0)
            throw new IllegalArgumentException("Null or empty consent bytes passed as an argument");

        final Bits bits = new Bits(bytes);
        final int version = getVersion(bits);
        switch (version) {
            case 1:
                return new ByteBufferBackedVendorConsent(bits);
            default:
                throw new IllegalStateException("Unsupported version: " + version);
        }
    }

    public static VendorConsent fromBase64String(String consentString, boolean validate) {
        if (isNullOrEmpty(consentString))
            throw new IllegalArgumentException("Null or empty consent string passed as an argument");

        return fromByteArray(BASE64_DECODER.decode(consentString), validate);
    }

    public static VendorConsent fromByteArray(byte[] bytes, boolean validate) {
        if (bytes == null || bytes.length == 0)
            throw new IllegalArgumentException("Null or empty consent bytes passed as an argument");

        final Bits bits = new Bits(bytes);
        final int version = getVersion(bits);
        switch (version) {
            case 1:
                if(validate) {
                    if (isValidate(bits)) {
                        return new ByteBufferBackedVendorConsent(bits);
                    } else {
                        throw new VendorConsentParseException("Found invalidate range entry");
                    }
                }
                return new ByteBufferBackedVendorConsent(bits);
            default:
                throw new IllegalStateException("Unsupported version: " + version);
        }
    }

    /**
     * Get the version field from bitmap
     * @param bits bitmap
     * @return a version number
     */
    private static int getVersion(Bits bits) {
        return bits.getInt(VERSION_BIT_OFFSET, VERSION_BIT_SIZE);
    }

    /**
     * Utility method to check whether string is empty or null
     * @param string value to check
     * @return a boolean value of the check
     */
    private static boolean isNullOrEmpty(String string) {
        return string == null || string.isEmpty();
    }

    /***
     * Utility method to check whether bits is validate or not
     * now it only checks the entries
     * @param bits value to check
     * @return true if validate, otherwise false
     */
    private static boolean isValidate(Bits bits){
        final int numEntries = bits.getInt(NUM_ENTRIES_OFFSET, NUM_ENTRIES_SIZE);
        final int maxVendorId = bits.getInt(MAX_VENDOR_ID_OFFSET, MAX_VENDOR_ID_SIZE);
        int currentOffset = RANGE_ENTRY_OFFSET;
        for (int i = 0; i < numEntries; i++) {
            boolean range = bits.getBit(currentOffset);
            currentOffset++;
            if (range) {
                int startVendorId = bits.getInt(currentOffset, VENDOR_ID_SIZE);
                currentOffset += VENDOR_ID_SIZE;
                int endVendorId = bits.getInt(currentOffset, VENDOR_ID_SIZE);
                currentOffset += VENDOR_ID_SIZE;
                if (startVendorId > endVendorId || endVendorId > maxVendorId) {
                    return false;
                }
            } else {
                int singleVendorId = bits.getInt(currentOffset, VENDOR_ID_SIZE);
                currentOffset += VENDOR_ID_SIZE;
                if (singleVendorId > maxVendorId) {
                    return false;
                }
            }
        }
        return true;
    }
}
