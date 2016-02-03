package cloudos.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.filters.Scrubbable;
import org.cobbzilla.wizard.filters.ScrubbableField;
import org.cobbzilla.wizard.model.HashedPassword;
import org.cobbzilla.wizard.model.UniquelyNamedEntity;
import org.cobbzilla.wizard.validation.HasValue;
import org.hibernate.validator.constraints.Email;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import javax.validation.constraints.Size;

import java.util.Comparator;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.daemon.ZillaRuntime.safeInt;
import static org.cobbzilla.util.reflect.ReflectionUtil.copy;

@MappedSuperclass @Accessors(chain=true)
public class AccountBase extends UniquelyNamedEntity implements Scrubbable, BasicAccount {

    public static final String EMAIL_VERIFICATION_CODE = "emailVerificationCode";
    public static final String RESET_PASSWORD_TOKEN = "resetPasswordToken";
    public static final Comparator<AccountBase> SORT_ACCOUNT_NAME = new Comparator<AccountBase>() {
        @Override public int compare(AccountBase a1, AccountBase a2) {
            return a1 == null ? 1 : a2 == null ? -1 : String.valueOf(a1.getName()).compareTo(String.valueOf(a2.getName()));
        }
    };

    @JsonIgnore public int getVerifyCodeLength () { return 16; }

    private static final ScrubbableField[] SCRUBBABLE_FIELDS = new ScrubbableField[]{
            new ScrubbableField(AccountBase.class, "authId", String.class)
    };

    @Override @JsonIgnore public ScrubbableField[] fieldsToScrub() { return SCRUBBABLE_FIELDS; }

    public static final String ERR_AUTHID_LENGTH = "{err.authid.length}";
    public static final String ERR_EMAIL_INVALID = "{err.email.invalid}";
    public static final String ERR_EMAIL_EMPTY = "{err.email.empty}";
    public static final String ERR_EMAIL_LENGTH = "{err.email.length}";
    public static final String ERR_LAST_NAME_EMPTY = "{err.lastName.empty}";
    public static final String ERR_LAST_NAME_LENGTH = "{err.lastName.length}";
    public static final String ERR_FIRST_NAME_EMPTY = "{err.firstName.empty}";
    public static final String ERR_FIRST_NAME_LENGTH = "{err.firstName.length}";
    public static final String ERR_MOBILEPHONE_LENGTH = "{err.mobilePhone.length}";
    public static final String ERR_MOBILEPHONE_EMPTY = "{err.mobilePhone.empty}";
    public static final String ERR_MOBILEPHONE_CC_EMPTY = "{err.mobilePhoneCountryCode.empty}";
    public static final String ERR_PRIMARY_GROUP_LENGTH = "{err.primaryGroup.length}";
    public static final String ERR_LOCALE_LENGTH = "{err.locale.length}";
    public static final int EMAIL_MAXLEN = 255;
    public static final int VERIFY_CODE_MAXLEN = 100;
    public static final int LASTNAME_MAXLEN = 25;
    public static final int FIRSTNAME_MAXLEN = 25;
    public static final int MOBILEPHONE_MAXLEN = 30;
    public static final int MOBILEPHONE_MINLEN = 8;
    public static final int PRIMARY_GROUP_MAXLEN = 100;
    public static final int LOCALE_MAXLEN = 40;

    @Getter @Setter @Embedded
    @JsonIgnore private HashedPassword hashedPassword;

    @Override public String initResetToken() { return hashedPassword.initResetToken(); }
    @Override @JsonIgnore public long getResetTokenAge() { return hashedPassword.getResetTokenAge(); }
    @Override public AccountBase setPassword(String newPassword) { hashedPassword.setPassword(newPassword); return this; }
    @Override public void setResetToken(String token) { hashedPassword.setResetToken(token); }

    @Size(max=30, message=ERR_AUTHID_LENGTH)
    @Getter @Setter private String authId = null;

    public boolean hasAuthId() { return !empty(authId); }

    @JsonIgnore @Transient public Integer getAuthIdInt() { return safeInt(authId); }
    public AccountBase setAuthIdInt(int authId) { setAuthId(String.valueOf(authId)); return this; }

    @Transient
    public String getAccountName () { return getName(); }
    public AccountBase setAccountName (String name) { setName(name); return this; }

    @HasValue(message=ERR_LAST_NAME_EMPTY)
    @Size(max=LASTNAME_MAXLEN, message=ERR_LAST_NAME_LENGTH)
    @Column(nullable=false, length=LASTNAME_MAXLEN)
    @Getter @Setter private String lastName;

    @HasValue(message=ERR_FIRST_NAME_EMPTY)
    @Size(max=FIRSTNAME_MAXLEN, message=ERR_FIRST_NAME_LENGTH)
    @Column(nullable=false, length=FIRSTNAME_MAXLEN)
    @Getter @Setter private String firstName;

    @JsonIgnore public String getFullName() { return getFirstName() + " " + getLastName(); }
    @JsonIgnore public String getLastNameFirstName() { return getLastName() + ", " + getFirstName(); }

    @Getter @Setter private boolean admin = false;
    @Getter @Setter private boolean suspended = false;
    @Getter @Setter private boolean twoFactor = false;

    @Getter @Setter private Long lastLogin = null;
    public void setLastLogin () { lastLogin = System.currentTimeMillis(); }

    @Email(message=ERR_EMAIL_INVALID)
    @HasValue(message=ERR_EMAIL_EMPTY)
    @Size(max=EMAIL_MAXLEN, message=ERR_EMAIL_LENGTH)
    @Column(unique=true, nullable=false, length=EMAIL_MAXLEN)
    @Getter private String email;

    @JsonIgnore @Size(max=VERIFY_CODE_MAXLEN) @Getter @Setter private String emailVerificationCode;
    @JsonIgnore @Getter @Setter private Long emailVerificationCodeCreatedAt;
    @Getter private boolean emailVerified = false;

    public String initEmailVerificationCode() {
        emailVerificationCode = randomAlphanumeric(getVerifyCodeLength());
        emailVerificationCodeCreatedAt = System.currentTimeMillis();
        return emailVerificationCode;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
        emailVerificationCode = null;
        emailVerificationCodeCreatedAt = null;
    }

    public boolean isEmailVerificationCodeValid (long expiration) {
        return emailVerificationCodeCreatedAt != null && emailVerificationCodeCreatedAt > (System.currentTimeMillis() - expiration);
    }

    public AccountBase setEmail (String email) {
        if (this.email == null || !this.email.equals(email)) {
            emailVerified = false;
            emailVerificationCode = null;
            emailVerificationCodeCreatedAt = null;
            this.email = email;
        }
        return this;
    }

    @Size(min=MOBILEPHONE_MINLEN, max=MOBILEPHONE_MAXLEN, message=ERR_MOBILEPHONE_LENGTH)
    @HasValue(message=ERR_MOBILEPHONE_EMPTY)
    @Getter private String mobilePhone;
    public AccountBase setMobilePhone (String mobilePhone) {
        if (this.mobilePhone == null || !this.mobilePhone.equals(mobilePhone)) {
            this.authId = null;
            this.mobilePhone = mobilePhone;
        }
        return this;
    }

    @HasValue(message=ERR_MOBILEPHONE_CC_EMPTY)
    @Getter private Integer mobilePhoneCountryCode;

    public AccountBase setMobilePhoneCountryCode(Integer mobilePhoneCountryCode) {
        if (this.mobilePhoneCountryCode == null || !this.mobilePhoneCountryCode.equals(mobilePhoneCountryCode)) {
            this.authId = null;
            this.mobilePhoneCountryCode = mobilePhoneCountryCode;
        }
        return this;
    }

    @JsonIgnore @Transient public String getMobilePhoneCountryCodeString() { return mobilePhoneCountryCode == null ? null : mobilePhoneCountryCode.toString(); }

    @Size(max=LOCALE_MAXLEN, message=ERR_LOCALE_LENGTH)
    @Getter @Setter private String locale;
    @JsonIgnore public boolean hasLocale () { return !empty(locale); }

    public AccountBase populate(AccountBase other) {
        setName(other.getName());
        setEmail(other.getEmail());
        setFirstName(other.getFirstName());
        setLastName(other.getLastName());
        setMobilePhone(other.getMobilePhone());
        setMobilePhoneCountryCode(other.getMobilePhoneCountryCode());
        setAdmin(other.isAdmin());
        setSuspended(other.isSuspended());
        setTwoFactor(other.isTwoFactor());
        setAuthId(other.getAuthId());
        if (other.getLastLogin() != null) setLastLogin(other.getLastLogin());
        if (getHashedPassword() != null && other.getHashedPassword() != null) {
            getHashedPassword().setResetToken(other.getHashedPassword().getResetToken());
        }
        setLocale(other.getLocale());
        return this;
    }

    @NoArgsConstructor
    public static class PublicView {
        @Getter @Setter public String name;
        @Getter @Setter public String firstName;
        @Getter @Setter public String lastName;
        @Getter @Setter public String fullName;
        public PublicView (AccountBase other) { copy(this, other); }
    }
}
