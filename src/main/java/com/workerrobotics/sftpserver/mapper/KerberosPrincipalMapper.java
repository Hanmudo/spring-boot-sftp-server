package com.workerrobotics.sftpserver.mapper;

import org.springframework.stereotype.Service;

@Service
public class KerberosPrincipalMapper {

    /**
     * Voorbeeld:
     *   ricky@EXAMPLE.LOCAL -> ricky
     *   ricky/admin@EXAMPLE.LOCAL -> ricky
     */
    public String toApplicationUsername(String kerberosPrincipal) {
        if (kerberosPrincipal == null || kerberosPrincipal.isBlank()) {
            throw new IllegalArgumentException("Kerberos principal is leeg");
        }

        String principalWithoutRealm = kerberosPrincipal;
        int at = kerberosPrincipal.indexOf('@');
        if (at >= 0) {
            principalWithoutRealm = kerberosPrincipal.substring(0, at);
        }

        int slash = principalWithoutRealm.indexOf('/');
        if (slash >= 0) {
            principalWithoutRealm = principalWithoutRealm.substring(0, slash);
        }

        return principalWithoutRealm;
    }
}
