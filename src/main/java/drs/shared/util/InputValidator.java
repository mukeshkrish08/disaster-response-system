package drs.shared.util;

import java.util.regex.Pattern;

/**
 * Reusable validation rules for user input. Used on both client (form
 * validation) and server (defence-in-depth).
  
 */
public final class InputValidator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$");

    private static final Pattern LATITUDE_OK = Pattern.compile(
            "^-?(?:90(?:\\.0+)?|[0-8]?\\d(?:\\.\\d+)?)$");

    private static final Pattern LONGITUDE_OK = Pattern.compile(
            "^-?(?:180(?:\\.0+)?|(?:1[0-7]\\d|\\d{1,2})(?:\\.\\d+)?)$");

    private InputValidator() {
        // Not instantiable
    }

    /*   * @param email candidate email
     * @return true if non-null, trimmed, and matches RFC-like pattern
     */
    public static boolean validateEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /*   * A password is valid when it has at least
     * {@link DrsConstants#MIN_PASSWORD_LENGTH} characters and contains an
     * uppercase letter, a digit, and a non-alphanumeric character.
         * @param password candidate password
     * @return true when the rules pass
     */
    public static boolean validatePassword(String password) {
        if (password == null || password.length() < DrsConstants.MIN_PASSWORD_LENGTH) {
            return false;
        }
        boolean upper = false;
        boolean digit = false;
        boolean special = false;
        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (Character.isUpperCase(c)) {
                upper = true;
            } else if (Character.isDigit(c)) {
                digit = true;
            } else if (!Character.isLetterOrDigit(c)) {
                special = true;
            }
        }
        return upper && digit && special;
    }

    /*   * @param description candidate description
     * @return true when length is within bounds and contains no obvious
     *        HTML/script tags
     */
    public static boolean validateDescription(String description) {
        if (description == null) {
            return false;
        }
        String trimmed = description.trim();
        if (trimmed.length() < DrsConstants.MIN_DESCRIPTION_LENGTH
                || trimmed.length() > DrsConstants.MAX_DESCRIPTION_LENGTH) {
            return false;
        }
        String lower = trimmed.toLowerCase();
        return !lower.contains("<script") && !lower.contains("</script");
    }

    /*   * @param location candidate location string
     * @return true when non-empty and not absurdly long
     */
    public static boolean validateLocation(String location) {
        if (location == null) {
            return false;
        }
        String trimmed = location.trim();
        return !trimmed.isEmpty() && trimmed.length() <= 200;
    }

    /*   * @param latitude candidate latitude
     * @return true when in range [-90, 90]
     */
    public static boolean validateLatitude(double latitude) {
        return latitude >= -90.0 && latitude <= 90.0;
    }

    /*   * @param longitude candidate longitude
     * @return true when in range [-180, 180]
     */
    public static boolean validateLongitude(double longitude) {
        return longitude >= -180.0 && longitude <= 180.0;
    }

    /*   * @param quantity proposed quantity to allocate
     * @return true when strictly positive
     */
    public static boolean validateQuantity(int quantity) {
        return quantity > 0;
    }

    /*   * Validate Australian phone number - mobile or landline. Accepts:
     *  0412 345 678  (with spaces)
     *  0412345678    (no spaces)
     *  +61 412 345 678  (international mobile)
     *  (02) 9876 5432  (landline with area-code parens)
     *  02 9876 5432  (landline, no parens)
     * Spaces, hyphens, and parentheses are tolerated and stripped
     * before the regex check. After stripping the number must be
     * 10 digits starting with 0 OR 11 digits starting with 61.
         * @param phone raw phone string from the form
     * @return true when phone is a recognisable AU mobile/landline
     */
    public static boolean isValidAuPhone(String phone) {
        if (phone == null) {
            return false;
        }
        // Strip whitespace, hyphens, parens, leading + sign
        String digits = phone.replaceAll("[\\s\\-()+]", "");
        if (digits.isEmpty()) {
            return false;
        }
        // National form: 10 digits starting with 0 (e.g. 02xxxxxxxx mobile or landline)
        if (digits.length() == 10 && digits.startsWith("0")) {
            // Second digit must be 2-9 (real area code or mobile prefix '4')
            char d2 = digits.charAt(1);
            return d2 >= '2' && d2 <= '9';
        }
        // International form: 61 followed by 9 digits where the leading 0 has been dropped
        if (digits.length() == 11 && digits.startsWith("61")) {
            char d3 = digits.charAt(2);
            return d3 >= '2' && d3 <= '9';
        }
        return false;
    }
}
