package sk.iway.iwcm.form.validators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.CollectionUtils;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.form.FormDB;
import sk.iway.iwcm.i18n.Prop;

public class PhoneValidator {

    private static final PhoneValidator VALIDATOR = new PhoneValidator();
    private Prop prop;
    public static PhoneValidator getInstance() {
        return VALIDATOR;
    }
    public Set<String> phoneCodes;
    public boolean onlyMobilePhone;

    protected PhoneValidator() {
        prop = Prop.getInstance();
    }

    public boolean isValid(List<String> phoneClasses, String phoneNumberIn) {
        String phoneNumber = phoneNumberIn;
        if (phoneNumber == null) {
            phoneNumber = "";
        }

        phoneNumber = normalize(phoneNumber);
        if (Tools.isEmpty(phoneNumber)) {
            return false;
        }

        if (onlyMobilePhone && !phoneNumber.startsWith("+4219")) {
            return false;
        }

        List<String> regexps = getRegexps(phoneClasses);
        String stringPattern = Tools.join(regexps, "|");
        Pattern pattern = Pattern.compile(stringPattern);
        Matcher matcher = pattern.matcher(phoneNumber);

        if (matcher.matches() == false) {
            return false;
        }

        return true;
    }

    protected List<String> getRegexps(List<String> phoneClasses) {
        List<String> regexps = new ArrayList<>();

        if (phoneClasses != null) {
            for (String phoneClass : phoneClasses) {
                String[] row = FormDB.getRegExpByType(phoneClass.trim());
                if (row != null) {
                    regexps.add(row[2]);
                }
            }
        }

        if (phoneCodes != null) {
            //regexps.add("^([+]|00)[0-9]{1,3}[0-9]{10}$|^0[0-9]{9}$");
            for (String phoneCode : phoneCodes) {
                if (phoneCode.startsWith("+")) {
                    phoneCode = phoneCode.substring(1);
                }

                if (phoneCode.startsWith("00")) {
                    phoneCode = phoneCode.substring(2);
                }

                regexps.add("^[+]" + phoneCode + "[+0-9]{9}$");
                regexps.add("^00" + phoneCode + "[+0-9]{9}$");
            }

            if (phoneCodes.contains("+421")) {
                regexps.add("^0[0-9]{9}");
            }
        }

        return regexps;
    }

    public boolean hasPhoneClass(List<String> classes) {
        return CollectionUtils.containsAny(classes, getPhoneClasses());
    }

    public boolean hasBlacklistedPhoneClass(List<String> classes) {
        return CollectionUtils.containsAny(classes, getBlacklistedPhoneClasses());
    }

    public boolean isBlacklisted(String phoneNumber) {
        String primaryKey = "tatrabanka.blacklisted_phone_numbers";
        String secondaryKey = "blacklisted_phone_numbers";
        String blacklist = getConstant(primaryKey, secondaryKey);

        if (Tools.isNotEmpty(blacklist)) {
            String phoneNumberNormalized = normalize(phoneNumber);
            String delimiter = blacklist.contains(",") ? "," : "|";
            List<String> blacklistItems = Arrays.asList(Tools.getTokens(blacklist,delimiter));

            if (blacklistItems.stream().anyMatch(phoneNumberNormalized::startsWith)) {
                return true;
            }
        }

        return false;
    }

    protected List<String> getBlacklistedPhoneClasses() {
        String primaryKey = "tatrabanka.blacklisted_phone_numbers.html_classes";
        String secondaryKey = "blacklisted_phone_numbers.html_classes";
        String classes = getConstant(primaryKey, secondaryKey);

        if (Tools.isNotEmpty(classes)) {
            String delimiter = classes.contains(",") ? "," : "|";
            return Arrays.asList(Tools.getTokens(classes, delimiter));
        }

        return Collections.emptyList();
    }

    public List<String> getPhoneClasses() {
        String primaryKey = "tatrabanka.phone_numbers.html_classes";
        String secondaryKey = "phone_numbers.html_classes";
        String classes = getConstant(primaryKey, secondaryKey);

        if (Tools.isNotEmpty(classes)) {
            String delimiter = classes.contains(",") ? "," : "|";
            return Arrays.asList(Tools.getTokens(classes, delimiter));
        }

        return Collections.emptyList();
    }

    protected String getConstant(String key, String defaultKey) {
        return Constants.getString(key, Constants.getString(defaultKey));
    }

    protected String getText(String key, String defaultKey) {
        return !prop.getText(key).equals(key) ? prop.getText(key) : !prop.getText(defaultKey).equals(defaultKey) ? prop.getText(defaultKey) : null;
    }

    private String normalize(String phoneNumber) {
        if (phoneNumber == null) {
            return "";
        }

        String result = phoneNumber;

        if (result.startsWith("00")) {
            result = "+" + result.substring(2);
        }
        else if (result.startsWith("0")) {
            result = "+421" + result.substring(1);
        }

        return result;
    }

    public String getPhoneNumberNormalized(String phoneNumber) {
        return normalize(phoneNumber);
    }

    public Set<String> getPhoneCodes() {
        return phoneCodes;
    }

    public void setPhoneCodes(Set<String> phoneCodes) {
        this.phoneCodes = phoneCodes;
    }

    public void setPhoneCodes(List<String> phoneCodes) {
        this.phoneCodes.clear();
        this.phoneCodes.addAll(phoneCodes);
    }

    public void addPhoneCode(String phoneCode) {
        if (phoneCodes == null) {
            phoneCodes = new HashSet<>();
        }
        this.phoneCodes.add(phoneCode);
    }

    public void addPhoneCodes(Set<String> phoneCodes) {
        if (this.phoneCodes == null) {
            this.phoneCodes = new HashSet<>();
        }
        this.phoneCodes.addAll(phoneCodes);
    }

    public void addPhoneCodes(List<String> phoneCodes) {
        if (this.phoneCodes == null) {
            this.phoneCodes = new HashSet<>();
        }
        this.phoneCodes.addAll(phoneCodes);
    }
}