package com.banquemisr.recruitment.Authentication.Security;


import com.banquemisr.recruitment.Authentication.Security.Property.LdapProperties;
import com.banquemisr.recruitment.exception.DuplicateResourceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ldap.NameAlreadyBoundException;
import org.springframework.ldap.NameNotFoundException;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.support.LdapNameBuilder;
import org.springframework.stereotype.Service;

import javax.naming.Name;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Owns the lifecycle of user identities in LDAP. LDAP is now the sole store of credentials -
 * the "users" table in Postgres only keeps application-level metadata (name, role, enabled flag).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LdapUserService {

    private final LdapTemplate ldapTemplate;
    private final LdapProperties ldapProperties;

    public void createUser(String uid, String firstName, String lastName, String email, String rawPassword) {
        Name dn = buildDn(uid);

        BasicAttribute objectClass = new BasicAttribute("objectClass");
        objectClass.add("top");
        objectClass.add("person");
        objectClass.add("organizationalPerson");
        objectClass.add("inetOrgPerson");

        Attributes attributes = new BasicAttributes();
        attributes.put(objectClass);
        attributes.put("uid", uid);
        attributes.put("cn", firstName + " " + lastName);
        attributes.put("sn", lastName);
        attributes.put("givenName", firstName);
        attributes.put("mail", email);
        attributes.put("userPassword", hashPassword(rawPassword));

        try {
            ldapTemplate.bind(dn, null, attributes);
        } catch (NameAlreadyBoundException ex) {
            throw new DuplicateResourceException("An LDAP entry already exists for user: " + email);
        } catch (Exception ex) {
            log.error("Failed to create LDAP entry for '{}': {}", email, ex.getMessage(), ex);
            throw new IllegalStateException("Failed to register user in LDAP directory: " + ex.getMessage(), ex);
        }
    }

    public void updatePassword(String uid, String newRawPassword) {
        Name dn = buildDn(uid);
        try {
            ldapTemplate.modifyAttributes(dn, new ModificationItem[]{
                    new ModificationItem(
                            DirContext.REPLACE_ATTRIBUTE,
                            new BasicAttribute("userPassword", hashPassword(newRawPassword)))
            });
        } catch (Exception ex) {
            log.error("Failed to update LDAP password for '{}': {}", uid, ex.getMessage(), ex);
            throw new IllegalStateException("Failed to update password in LDAP directory: " + ex.getMessage(), ex);
        }
    }

    public void deleteUser(String uid) {
        Name dn = buildDn(uid);
        try {
            ldapTemplate.unbind(dn);
        } catch (NameNotFoundException ex) {
            log.warn("LDAP entry for '{}' was already absent during delete", uid);
        } catch (Exception ex) {
            log.error("Failed to delete LDAP entry for '{}': {}", uid, ex.getMessage(), ex);
            throw new IllegalStateException("Failed to delete user from LDAP directory: " + ex.getMessage(), ex);
        }
    }

    public boolean exists(String uid) {
        try {
            return ldapTemplate.lookup(buildDn(uid)) != null;
        } catch (NameNotFoundException ex) {
            return false;
        }
    }

    private Name buildDn(String uid) {
        return LdapNameBuilder.newInstance()
                .add(ldapProperties.getPeopleOu())
                .add("uid", uid)
                .build();
    }


    private byte[] hashPassword(String rawPassword) {
        try {
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[8];
            random.nextBytes(salt);

            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(rawPassword.getBytes(StandardCharsets.UTF_8));
            digest.update(salt);
            byte[] hash = digest.digest();

            byte[] hashPlusSalt = new byte[hash.length + salt.length];
            System.arraycopy(hash, 0, hashPlusSalt, 0, hash.length);
            System.arraycopy(salt, 0, hashPlusSalt, hash.length, salt.length);

            return ("{SSHA}" + Base64.getEncoder().encodeToString(hashPlusSalt))
                    .getBytes(StandardCharsets.UTF_8);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 not available for LDAP password hashing", e);
        }
    }
}