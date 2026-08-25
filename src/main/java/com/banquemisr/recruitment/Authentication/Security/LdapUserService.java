package com.banquemisr.recruitment.Authentication.Security;

import com.banquemisr.recruitment.Authentication.Security.Property.LdapProperties;
import com.banquemisr.recruitment.exception.DuplicateResourceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ldap.NameAlreadyBoundException;
import org.springframework.ldap.NameNotFoundException;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.DirContextAdapter;
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
import java.util.List;

import static org.springframework.ldap.query.LdapQueryBuilder.query;

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
        attributes.put("userPassword", rawPassword);

        try {
            ldapTemplate.bind(dn, null, attributes);
        } catch (NameAlreadyBoundException ex) {
            throw new DuplicateResourceException("An LDAP entry already exists for user: " + email);
        } catch (Exception ex) {
            log.error("Failed to create LDAP entry for '{}': {}", email, ex.getMessage(), ex);
            throw new IllegalStateException("Failed to register user in LDAP directory: " + ex.getMessage(), ex);
        }
    }

    public void updatePassword(String identifier, String newRawPassword) {
        Name dn = resolveDn(identifier);
        try {
            ldapTemplate.modifyAttributes(dn, new ModificationItem[]{
                    new ModificationItem(
                            DirContext.REPLACE_ATTRIBUTE,
                            new BasicAttribute("userPassword", newRawPassword))
            });
        } catch (Exception ex) {
            log.error("Failed to update LDAP password for '{}': {}", identifier, ex.getMessage(), ex);
            throw new IllegalStateException("Failed to update password in LDAP directory: " + ex.getMessage(), ex);
        }
    }

    public void deleteUser(String identifier) {
        Name dn = resolveDn(identifier);
        try {
            ldapTemplate.unbind(dn);
        } catch (NameNotFoundException ex) {
            log.warn("LDAP entry for '{}' was already absent during delete", identifier);
        } catch (Exception ex) {
            log.error("Failed to delete LDAP entry for '{}': {}", identifier, ex.getMessage(), ex);
            throw new IllegalStateException("Failed to delete user from LDAP directory: " + ex.getMessage(), ex);
        }
    }

    public boolean exists(String identifier) {
        try {
            Name dn = resolveDn(identifier);
            return ldapTemplate.lookup(dn) != null;
        } catch (NameNotFoundException ex) {
            return false;
        }
    }

    private Name resolveDn(String identifier) {
        try {
            List<String> uids = ldapTemplate.search(
                    query().base(ldapProperties.getPeopleOu())
                            .where("mail").is(identifier)
                            .or("uid").is(identifier),
                    (ContextMapper<String>) ctx -> {
                        DirContextAdapter adapter = (DirContextAdapter) ctx;
                        return adapter.getStringAttribute("uid");
                    }
            );
            if (uids != null && !uids.isEmpty() && uids.get(0) != null) {
                return buildDn(uids.get(0));
            }
        } catch (Exception ex) {
            log.warn("Could not resolve DN via LDAP search for '{}', falling back to direct DN: {}", identifier, ex.getMessage());
        }
        return buildDn(identifier);
    }

    private Name buildDn(String uid) {
        return LdapNameBuilder.newInstance()
                .add(ldapProperties.getPeopleOu())
                .add("uid", uid)
                .build();
    }
}