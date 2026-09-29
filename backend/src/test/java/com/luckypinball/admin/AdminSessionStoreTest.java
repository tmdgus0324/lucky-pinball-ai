package com.luckypinball.admin;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AdminSessionStoreTest {

    @Test
    void issuedTokenIsValidUntilRevoked() {
        AdminSessionStore store = new AdminSessionStore();

        String token = store.issue();

        assertNotNull(token);
        assertTrue(store.isValid(token));

        store.revoke(token);

        assertFalse(store.isValid(token));
    }

    @Test
    void unknownTokenIsNeverValid() {
        AdminSessionStore store = new AdminSessionStore();

        assertFalse(store.isValid("no-such-token"));
        assertFalse(store.isValid(null));
    }

    @Test
    void differentIssuesProduceDifferentTokens() {
        AdminSessionStore store = new AdminSessionStore();

        String first = store.issue();
        String second = store.issue();

        assertTrue(store.isValid(first));
        assertTrue(store.isValid(second));
        assertNotNull(first);
        assertNotNull(second);
        org.junit.jupiter.api.Assertions.assertNotEquals(first, second);
    }
}
